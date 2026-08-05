"use client";

import { API_BASE_URL } from "@/config/api";

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

        if (!response.ok) return null;

        return await response.json();
      } catch {
        return null;
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

        return response.ok;
      } catch {
        return false;
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
