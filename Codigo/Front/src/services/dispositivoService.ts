"use client";

import { API_BASE_URL } from "@/config/api";
import { getAuthHeaders } from "@/utils/httpUtils";

/**
 * Chave do device token no localStorage do celular — compartilhada entre a página pública de
 * pareamento (que grava) e qualquer tela de captura (que lê, ver `X-Device-Token` no upload).
 * Centralizada aqui pra nunca dessincronizar entre os dois lugares.
 */
export const DEVICE_TOKEN_STORAGE_KEY = "hortifruti_device_token";

export interface Dispositivo {
  id: number;
  nome: string;
  pareadoEm: string;
  ultimoUsoEm: string | null;
}

export interface PareamentoIniciado {
  codigo: string;
  expiraEm: string;
}

export interface PareamentoConfirmado {
  deviceToken: string;
  dispositivoId: number;
}

/**
 * `DispositivoController`/`NotaController` (diferente da maioria dos outros controllers, ex.
 * `/clients`) já incluem "/api" no próprio `@RequestMapping`. Como `API_BASE_URL` é o prefixo do
 * rewrite same-origin do Next (`/api`, ver next.config.ts), o caminho aqui precisa repetir "/api"
 * pra virar `/api/api/dispositivos/...` no browser — o rewrite descarta o primeiro `/api` e repassa
 * `/api/dispositivos/...` pro backend, que é o que o `@RequestMapping("/api/dispositivos")` espera.
 */
const DISPOSITIVOS_PATH = `${API_BASE_URL}/api/dispositivos`;

async function extrairMensagemErro(
  response: Response,
  fallback: string,
): Promise<string> {
  const body = await response.json().catch(() => null);
  return body?.message || body?.error || fallback;
}

export const dispositivoService = {
  /** PC, autenticado normalmente: gera o código de 6 dígitos exibido/QR desta sessão. */
  async iniciarPareamento(): Promise<PareamentoIniciado> {
    const response = await fetch(`${DISPOSITIVOS_PATH}/pareamento/iniciar`, {
      method: "POST",
      headers: getAuthHeaders(),
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao gerar código de pareamento.",
        ),
      );
    }

    return response.json();
  },

  /** Celular, sem sessão: confirma o código e recebe o device token (salvo pelo chamador). */
  async confirmarPareamento(
    codigo: string,
    nomeDispositivo: string,
  ): Promise<PareamentoConfirmado> {
    const response = await fetch(`${DISPOSITIVOS_PATH}/pareamento/confirmar`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify({ codigo, nomeDispositivo }),
    });

    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao confirmar pareamento."),
      );
    }

    return response.json();
  },

  /** PC, autenticado normalmente: lista os dispositivos vinculados ao usuário logado. */
  async listarDispositivos(): Promise<Dispositivo[]> {
    const response = await fetch(DISPOSITIVOS_PATH, {
      method: "GET",
      headers: getAuthHeaders(),
      credentials: "include",
      cache: "no-store",
    });

    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao carregar dispositivos."),
      );
    }

    return response.json();
  },

  /** PC, autenticado normalmente: revoga — a próxima captura daquele celular passa a falhar. */
  async revogarDispositivo(id: number): Promise<void> {
    const response = await fetch(`${DISPOSITIVOS_PATH}/${id}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao revogar dispositivo."),
      );
    }
  },
};
