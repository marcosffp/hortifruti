'use client';

import { authService } from './authService';

// Serviço para gerenciar usuários do sistema - adequado ao backend existente
interface UserRequest {
  username: string;
  password: string;
  role: "MANAGER" | "EMPLOYEE";
}

interface UserResponse {
  id: number;
  username: string;
  role: "MANAGER" | "EMPLOYEE";
}

// Função auxiliar para obter os headers com autorização
const getAuthHeaders = () => {
  const token = authService.getToken();
  
  if (token) {
    console.log('[DEBUG] Token preview:', token.substring(0, 50) + '...');
    
    // Verificar se o token está expirado
    if (authService.isTokenExpired(token)) {
      console.log('[DEBUG] Token is expired!');
      // Redirecionar para login se o token estiver expirado
      window.location.href = '/login';
      throw new Error('Token expirado. Redirecionando para login.');
    }
  }
  
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  return headers;
};

class UserService {
  private baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

  // Criar novo usuário
  async createUser(userData: UserRequest): Promise<UserResponse> {
    try {
      const response = await fetch(`${this.baseURL}/users/register`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(userData),
      });

      if (!response.ok) {
        throw new Error(`Erro ao criar usuário: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Falha ao criar usuário:', error);
      throw error;
    }
  }

  // Buscar todos os usuários
  async getAllUsers(): Promise<UserResponse[]> {
    try {
      const response = await fetch(`${this.baseURL}/users/all`, {
        method: 'GET',
        headers: getAuthHeaders(),
        cache: 'no-store',
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar usuários: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Falha ao obter usuários:', error);
      throw error;
    }
  }

  // Buscar usuário por username (não há endpoint por ID no backend)
  async getUserByUsername(username: string): Promise<UserResponse | null> {
    try {
      const users = await this.getAllUsers();
      return users.find(user => user.username === username) || null;
    } catch (error) {
      console.error(`Falha ao obter usuário ${username}:`, error);
      throw error;
    }
  }

  // Atualizar usuário por username
  async updateUser(userData: UserRequest): Promise<UserResponse> {
    try {
      const response = await fetch(`${this.baseURL}/users/update`, {
        method: 'PUT',
        headers: getAuthHeaders(),
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

  // Atualizar usuário por ID
  async updateUserById(id: number, userData: UserRequest): Promise<UserResponse> {
    try {
      const response = await fetch(`${this.baseURL}/users/update/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
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

  // Excluir usuário
  async deleteUser(username: string): Promise<void> {
    try {
      const response = await fetch(`${this.baseURL}/users/delete/${encodeURIComponent(username)}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });

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
