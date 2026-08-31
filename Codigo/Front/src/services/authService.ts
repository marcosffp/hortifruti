"use client";

import { API_BASE_URL } from "@/config/api";
import { sanitizeErrorMessage } from "@/utils/sanitizeErrorMessage";

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

/**
 * Resultado de `me()`/`refresh()` — deliberadamente com três estados, não um booleano/`null`.
 * Colapsar "sem sessão" e "backend indisponível" no mesmo valor falsy foi o bug central da
 * auditoria de sessão/autenticação (achado F1): um 503/timeout durante um cold start do Railway
 * virava, na prática, "usuário deslogado". Aqui:
 * - "authenticated": sessão válida, veio usuário.
 * - "unauthenticated": o backend respondeu e disse explicitamente que não há sessão válida
 *   (`/auth/me` sem token retorna 200 com corpo `null`; token inválido/expirado retorna 401 — ver
 *   TokenException.getHttpStatus() no backend). Só aqui vale a pena desistir e mandar pro login.
 * - "unavailable": qualquer outra coisa (5xx, 429, erro de rede/timeout) — o chamador deve tratar
 *   como "tente de novo depois", nunca como logout.
 */
export type SessionCheckResult =
  | { status: "authenticated"; user: AuthUser }
  | { status: "unauthenticated" }
  | { status: "unavailable" };

let pendingMeRequest: Promise<SessionCheckResult> | null = null;
let pendingRefreshRequest: Promise<SessionCheckResult> | null = null;

async function parseSessionResponse(
  response: Response,
): Promise<SessionCheckResult> {
  if (response.status === 401) return { status: "unauthenticated" };
  if (!response.ok) return { status: "unavailable" };

  const user = await response.json().catch(() => null);
  return user
    ? { status: "authenticated", user }
    : { status: "unauthenticated" };
}

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
        const fallback = `Erro ao fazer login: ${response.status}`;
        throw new LoginError(
          sanitizeErrorMessage(errorData.message || errorData.erro, fallback),
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

  /** GET /auth/me sempre responde (200 com usuário, 200 com `null`, ou 401) — nunca precisa de
   * corpo de erro especial: qualquer outra coisa (5xx, 429, erro de rede) é indisponibilidade. */
  async me(): Promise<SessionCheckResult> {
    if (pendingMeRequest) return pendingMeRequest;

    pendingMeRequest = (async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/me`, {
          method: "GET",
          credentials: "include",
        });

        return await parseSessionResponse(response);
      } catch {
        return { status: "unavailable" } as const;
      } finally {
        pendingMeRequest = null;
      }
    })();

    return pendingMeRequest;
  },

  async refresh(): Promise<SessionCheckResult> {
    if (pendingRefreshRequest) return pendingRefreshRequest;

    pendingRefreshRequest = (async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
          method: "POST",
          credentials: "include",
        });

        return await parseSessionResponse(response);
      } catch {
        return { status: "unavailable" } as const;
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
