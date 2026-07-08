"use client";

export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: {
    id: number;
    username: string;
    name: string;
    roles: string[];
  };
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const authService = {
  async login(credentials: AuthRequest): Promise<AuthResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/auth`, {
        method: "POST",
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

      const token = await response.text();

      // JWT tokens são divididos em três partes separadas por ponto: header.payload.signature
      const parts = token.split(".");
      if (parts.length !== 3) {
        throw new Error("Token inválido");
      }

      const payload = JSON.parse(atob(parts[1]));

      const user = {
        id: payload.id || 0,
        username: payload.sub || "",
        name: payload.sub || "", // Usar o subject como nome caso não haja um nome específico
        roles: [payload.role?.replace("ROLE_", "") || ""],
      };

      localStorage.setItem("auth_token", token);
      localStorage.setItem("user_info", JSON.stringify(user));

      return { token, user };
    } catch (error) {
      console.error("Falha ao fazer login:", error);
      throw error;
    }
  },

  logout() {
    localStorage.removeItem("auth_token");
    localStorage.removeItem("user_info");
    window.location.href = "/";
  },

  isTokenExpired(token: string): boolean {
    try {
      const parts = token.split(".");
      if (parts.length !== 3) return true;
      const payload = JSON.parse(atob(parts[1]));
      if (!payload.exp) return true;
      const now = Math.floor(Date.now() / 1000);
      return payload.exp < now;
    } catch {
      return true;
    }
  },

  isAuthenticated(): boolean {
    if (typeof window === "undefined") return false;
    const token = localStorage.getItem("auth_token");
    if (!token) return false;
    return !this.isTokenExpired(token);
  },

  getToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("auth_token");
  },

  getUserInfo() {
    if (typeof window === "undefined") return null;
    const userInfo = localStorage.getItem("user_info");
    return userInfo ? JSON.parse(userInfo) : null;
  },

  // Alias para getUserInfo para compatibilidade
  getCurrentUser() {
    return this.getUserInfo();
  },

  hasRole(role: string): boolean {
    const userInfo = this.getUserInfo();
    if (!userInfo?.roles) return false;
    return userInfo.roles.includes(role);
  },
};
