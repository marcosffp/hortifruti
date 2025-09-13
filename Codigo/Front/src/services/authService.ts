"use client";

// Tipos para o serviço de autenticação
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

// Definindo a URL base da API
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// Serviço para lidar com autenticação
export const authService = {
  // Login de usuário
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

      // Decodificar o token para extrair as informações do usuário
      // JWT tokens são divididos em três partes separadas por ponto: header.payload.signature
      const parts = token.split(".");
      if (parts.length !== 3) {
        throw new Error("Token inválido");
      }

      // Decodificar a parte do payload (segunda parte)
      const payload = JSON.parse(atob(parts[1]));

      const user = {
        id: payload.id || 0,
        username: payload.sub || "",
        name: payload.sub || "", // Usar o subject como nome caso não haja um nome específico
        roles: [payload.role?.replace("ROLE_", "") || ""],
      };

      // Armazenar o token no localStorage
      localStorage.setItem("auth_token", token);
      localStorage.setItem("user_info", JSON.stringify(user));

      return { token, user };
    } catch (error) {
      console.error("Falha ao fazer login:", error);
      throw error;
    }
  },

  // Logout de usuário
  logout() {
    localStorage.removeItem("auth_token");
    localStorage.removeItem("user_info");
    // Redirecionar para a página de login ou home, conforme necessário
    window.location.href = "/";
  },

  // Verificar se o token está expirado
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

  // Verificar se o usuário está autenticado e o token é válido
  isAuthenticated(): boolean {
    if (typeof window === "undefined") return false;
    const token = localStorage.getItem("auth_token");
    if (!token) return false;
    return !this.isTokenExpired(token);
  },

  // Obter o token atual
  getToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("auth_token");
  },

  // Obter informações do usuário
  getUserInfo() {
    if (typeof window === "undefined") return null;
    const userInfo = localStorage.getItem("user_info");
    return userInfo ? JSON.parse(userInfo) : null;
  },

  // Verificar se o usuário tem uma role específica
  hasRole(role: string): boolean {
    const userInfo = this.getUserInfo();
    if (!userInfo?.roles) return false;
    return userInfo.roles.includes(role);
  },
};
