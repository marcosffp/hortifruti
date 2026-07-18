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
}

let pendingMeRequest: Promise<AuthUser | null> | null = null;

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
        throw new Error(
          errorData.erro || `Erro ao fazer login: ${response.status}`,
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
