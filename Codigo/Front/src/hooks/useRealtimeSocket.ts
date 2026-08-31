"use client";

import { useEffect, useRef } from "react";
import { API_BASE_URL } from "@/config/api";

const WS_BASE_URL = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080";
const RECONECTAR_BASE_MS = 3000;
/**
 * Teto do backoff exponencial de reconexão — sem isso, uma indisponibilidade prolongada do
 * backend (ex.: hibernação/restart longo no Railway) faria o atraso entre tentativas crescer sem
 * limite (3s, 6s, 12s, 24s, 48s, ...), demorando cada vez mais para notar quando o backend volta.
 */
const RECONECTAR_MAX_MS = 30000;

export type RealtimeEvent =
  | { type: "dispositivo-pareado" }
  | { type: "captura-atualizada"; capturaId: number; status: string };

/**
 * Canal único de eventos em tempo real (pareamento de dispositivo + fila de notas — ver
 * `RealtimeNotificationRegistry` no backend). Conecta direto no backend (não pelo proxy de
 * rewrites do Next em `/api`) de propósito: a compressão gzip embutida do Next precisa acumular um
 * bloco inteiro de dados antes de liberar qualquer byte, o que quebra esse tipo de conexão
 * persistente. Em produção, `NEXT_PUBLIC_WS_URL` precisa apontar pra `wss://<domínio-do-backend>`
 * — sem isso, o fallback de dev (`ws://localhost:8080`) nunca conecta fora do ambiente local, e a
 * tela some no reconector silenciosamente (sem quebrar a página: quem usa este hook deve continuar
 * funcionando via polling manual/refresh mesmo sem o socket).
 *
 * Como o WebSocket conecta num domínio diferente do front, o cookie `auth_token` (emitido pro
 * domínio do front, via rewrite same-origin do Next) nunca chega no handshake — por isso busca um
 * ticket de uso único autenticado por cookie (`/realtime/ws-ticket`, same-origin) antes de abrir a
 * conexão, e manda o ticket na query string. Ver `RealtimeTicketService` no backend.
 *
 * Diferente de EventSource, WebSocket não reconecta sozinho — sem isso, uma queda de rede ou um
 * restart do backend deixaria a tela "em tempo real" parada pra sempre até um F5 manual.
 *
 * Reconexão usa backoff exponencial (base `RECONECTAR_BASE_MS`, teto `RECONECTAR_MAX_MS`),
 * resetado a cada conexão bem-sucedida — uma indisponibilidade prolongada do backend não deveria
 * fazer o cliente martelar `/realtime/ws-ticket` a cada 3s indefinidamente.
 */
export function useRealtimeSocket(onEvent: (event: RealtimeEvent) => void) {
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  useEffect(() => {
    let socket: WebSocket | null = null;
    let reconectarTimeoutId: ReturnType<typeof setTimeout> | null = null;
    let desmontado = false;
    let tentativas = 0;

    const agendarReconexao = () => {
      if (desmontado) return;
      const atraso = Math.min(
        RECONECTAR_BASE_MS * 2 ** tentativas,
        RECONECTAR_MAX_MS,
      );
      tentativas += 1;
      reconectarTimeoutId = setTimeout(conectar, atraso);
    };

    const conectar = async () => {
      let ticket: string;
      try {
        const response = await fetch(`${API_BASE_URL}/realtime/ws-ticket`, {
          method: "POST",
          credentials: "include",
        });
        if (!response.ok)
          throw new Error("Falha ao obter ticket de tempo real");
        ({ ticket } = await response.json());
      } catch {
        agendarReconexao();
        return;
      }

      if (desmontado) return;

      socket = new WebSocket(
        `${WS_BASE_URL}/ws/realtime?ticket=${encodeURIComponent(ticket)}`,
      );

      socket.onopen = () => {
        tentativas = 0;
      };

      socket.onmessage = (event) => {
        try {
          onEventRef.current(JSON.parse(event.data));
        } catch {
          // Mensagem que não é um evento reconhecido — ignora.
        }
      };

      socket.onclose = () => {
        agendarReconexao();
      };

      socket.onerror = () => socket?.close();
    };

    conectar();

    return () => {
      desmontado = true;
      if (reconectarTimeoutId) clearTimeout(reconectarTimeoutId);
      socket?.close();
    };
  }, []);
}
