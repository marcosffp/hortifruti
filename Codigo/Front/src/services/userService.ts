"use client";

import { API_BASE_URL } from "@/config/api";

interface UserRequest {
  username: string;
  password: string;
  position: string;
  role: "MANAGER" | "EMPLOYEE";
}

interface UserResponse {
  id: number;
  username: string;
  position: string;
  role: "MANAGER" | "EMPLOYEE";
}

const getAuthHeaders = (): HeadersInit => ({
  "Content-Type": "application/json",
});

class UserService {
  private baseURL = API_BASE_URL;

  async createUser(userData: UserRequest): Promise<UserResponse> {
    try {
      const response = await fetch(`${this.baseURL}/users/register`, {
        method: "POST",
        headers: getAuthHeaders(),
        credentials: "include",
        body: JSON.stringify(userData),
      });

      if (!response.ok) {
        throw new Error(`Erro ao criar usuário: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error("Falha ao criar usuário:", error);
      throw error;
    }
  }

  async getAllUsers(): Promise<UserResponse[]> {
    try {
      const response = await fetch(`${this.baseURL}/users/all`, {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "include",
        cache: "no-store",
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar usuários: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error("Falha ao obter usuários:", error);
      throw error;
    }
  }

  // Buscar usuário por username (não há endpoint por ID no backend)
  async getUserByUsername(username: string): Promise<UserResponse | null> {
    try {
      const users = await this.getAllUsers();
      return users.find((user) => user.username === username) || null;
    } catch (error) {
      console.error(`Falha ao obter usuário ${username}:`, error);
      throw error;
    }
  }

  async updateUser(userData: UserRequest): Promise<UserResponse> {
    try {
      const response = await fetch(`${this.baseURL}/users/update`, {
        method: "PUT",
        headers: getAuthHeaders(),
        credentials: "include",
        body: JSON.stringify(userData),
      });

      if (!response.ok) {
        throw new Error(`Erro ao atualizar usuário: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`Falha ao atualizar usuário:`, error);
      throw error;
    }
  }

  async updateUserById(
    id: number,
    userData: UserRequest,
  ): Promise<UserResponse> {
    try {
      const response = await fetch(`${this.baseURL}/users/update/${id}`, {
        method: "PUT",
        headers: getAuthHeaders(),
        credentials: "include",
        body: JSON.stringify(userData),
      });

      if (!response.ok) {
        throw new Error(`Erro ao atualizar usuário: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`Falha ao atualizar usuário ${id}:`, error);
      throw error;
    }
  }

  async deleteUser(username: string): Promise<void> {
    try {
      const response = await fetch(
        `${this.baseURL}/users/delete/${encodeURIComponent(username)}`,
        {
          method: "DELETE",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(`Erro ao excluir usuário: ${response.status}`);
      }
    } catch (error) {
      console.error(`Falha ao excluir usuário ${username}:`, error);
      throw error;
    }
  }
}

export const userService = new UserService();
export type { UserRequest, UserResponse };
