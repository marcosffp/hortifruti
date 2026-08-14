"use client";

import { API_BASE_URL_WITH_API_PREFIX } from "@/config/api";
import { getAuthHeaders } from "@/utils/httpUtils";

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
  dispositivoId: number;
}

export interface PareamentoStatus {
  pareado: boolean;
}

const DISPOSITIVOS_PATH = `${API_BASE_URL_WITH_API_PREFIX}/dispositivos`;

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

  /**
   * Celular, sem sessão: confirma o código. O backend grava o device token num cookie
   * `httpOnly` (nunca chega no corpo da resposta nem no JS) — ver Área C, item C-V5 da
   * AUDITORIA.md.
   */
  async confirmarPareamento(
    codigo: string,
    nomeDispositivo: string,
  ): Promise<PareamentoConfirmado> {
    const response = await fetch(`${DISPOSITIVOS_PATH}/pareamento/confirmar`, {
      method: "POST",
      headers: getAuthHeaders(),
      credentials: "include",
      body: JSON.stringify({ codigo, nomeDispositivo }),
    });

    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(response, "Falha ao confirmar pareamento."),
      );
    }

    return response.json();
  },

  /**
   * Celular, sem sessão: como o device token é `httpOnly`, o JS não consegue checar sozinho se
   * o cookie salvo ainda é válido — esta chamada pergunta pro backend (mesmo padrão de
   * `GET /auth/me` pro cookie de sessão).
   */
  async statusPareamento(): Promise<PareamentoStatus> {
    const response = await fetch(`${DISPOSITIVOS_PATH}/pareamento/status`, {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    });

    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao verificar vínculo do dispositivo.",
        ),
      );
    }

    return response.json();
  },

  /**
   * Celular, sem sessão: limpa o cookie `device_token` deste aparelho pra permitir vincular com
   * outro código. Não revoga o dispositivo pros outros — isso é `revogarDispositivo`, só
   * acessível do PC autenticado.
   */
  async desvincularLocal(): Promise<void> {
    const response = await fetch(
      `${DISPOSITIVOS_PATH}/pareamento/desvincular`,
      {
        method: "POST",
        credentials: "include",
      },
    );

    if (!response.ok) {
      throw new Error(
        await extrairMensagemErro(
          response,
          "Falha ao desvincular dispositivo.",
        ),
      );
    }
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
