"use client";

import { API_BASE_URL } from "@/config/api";
import { markRefreshed } from "@/utils/authRefreshCoordinator";

export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthUser {
  id: number;
  username: string;
  name: string;
  roles: string[];
  environment: string;
}

/**
 * retryAfter (segundos) vem preenchido quando o backend recusa o login por lockout
 * (proteção contra brute-force) — usado para desabilitar o botão de login até o
 * bloqueio expirar, sem expor "conta bloqueada" como texto para o usuário.
 */
export class LoginError extends Error {
  retryAfter?: number;

  constructor(message: string, retryAfter?: number) {
    super(message);
    this.name = "LoginError";
    this.retryAfter = retryAfter;
  }
}

let pendingMeRequest: Promise<AuthUser | null> | null = null;
let pendingRefreshRequest: Promise<boolean> | null = null;

/**
 * Lançado quando não foi possível confirmar o estado de autenticação (429 de rate limit, rede
 * fora do ar). Diferente de um 401/403, isso não significa "sessão inválida" — quem chama deve
 * manter o estado atual em vez de deslogar o usuário.
 */
export class TransientAuthCheckError extends Error {}

export const authService = {
  async login(credentials: AuthRequest): Promise<AuthUser> {
    try {
      const response = await fetch(`${API_BASE_URL}/auth`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(credentials),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new LoginError(
          errorData.message ||
            errorData.erro ||
            `Erro ao fazer login: ${response.status}`,
          typeof errorData.retryAfter === "number"
            ? errorData.retryAfter
            : undefined,
        );
      }

      return await response.json();
    } catch (error) {
      console.error("Falha ao fazer login:", error);
      throw error;
    }
  },

  async me(): Promise<AuthUser | null> {
    if (pendingMeRequest) return pendingMeRequest;

    pendingMeRequest = (async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/me`, {
          method: "GET",
          credentials: "include",
        });

        if (response.status === 429) {
          throw new TransientAuthCheckError(
            "Limite de requisições atingido ao verificar sessão.",
          );
        }

        if (!response.ok) return null;

        try {
          return await response.json();
        } catch {
          // Corpo vazio/truncado costuma ser uma navegação concorrente abortando a resposta em
          // andamento (ex.: redirect pro /login disparado por outra chamada) — não é uma resposta
          // "não autenticado" de verdade.
          throw new TransientAuthCheckError(
            "Resposta de /auth/me incompleta ao verificar sessão.",
          );
        }
      } finally {
        pendingMeRequest = null;
      }
    })();

    return pendingMeRequest;
  },

  async refresh(): Promise<boolean> {
    if (pendingRefreshRequest) return pendingRefreshRequest;

    pendingRefreshRequest = (async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
          method: "POST",
          credentials: "include",
        });

        if (response.status === 429) {
          throw new TransientAuthCheckError(
            "Limite de requisições atingido ao renovar sessão.",
          );
        }

        if (response.ok) markRefreshed();

        return response.ok;
      } finally {
        pendingRefreshRequest = null;
      }
    })();

    return pendingRefreshRequest;
  },

  async logout() {
    try {
      await fetch(`${API_BASE_URL}/auth/logout`, {
        method: "POST",
        credentials: "include",
      });
    } catch (error) {
      console.error("Falha ao encerrar sessão no servidor:", error);
    } finally {
      window.location.href = "/";
    }
  },
};
