// Serviço para gerenciar usuários do sistema
interface UserRequest {
  name: string;
  email: string;
  cargo: string;
  perfil: "Gestor" | "Funcionário";
  password: string;
}

interface UserResponse {
  id: number;
  name: string;
  email: string;
  cargo: string;
  perfil: "Gestor" | "Funcionário";
  createdAt: string;
  updatedAt: string;
}

class UserService {
  private baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

  // Criar novo usuário
  async createUser(userData: UserRequest): Promise<UserResponse> {
    const response = await fetch(`${this.baseURL}/api/users`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify(userData),
    });

    if (!response.ok) {
      throw new Error('Erro ao criar usuário');
    }

    return response.json();
  }

  // Buscar todos os usuários
  async getAllUsers(): Promise<UserResponse[]> {
    const response = await fetch(`${this.baseURL}/api/users`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    });

    if (!response.ok) {
      throw new Error('Erro ao buscar usuários');
    }

    return response.json();
  }

  // Buscar usuário por ID
  async getUserById(id: number): Promise<UserResponse> {
    const response = await fetch(`${this.baseURL}/api/users/${id}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    });

    if (!response.ok) {
      throw new Error('Erro ao buscar usuário');
    }

    return response.json();
  }

  // Atualizar usuário
  async updateUser(id: number, userData: Partial<UserRequest>): Promise<UserResponse> {
    const response = await fetch(`${this.baseURL}/api/users/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify(userData),
    });

    if (!response.ok) {
      throw new Error('Erro ao atualizar usuário');
    }

    return response.json();
  }

  // Excluir usuário
  async deleteUser(id: number): Promise<void> {
    const response = await fetch(`${this.baseURL}/api/users/${id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    });

    if (!response.ok) {
      throw new Error('Erro ao excluir usuário');
    }
  }
}

export const userService = new UserService();
export type { UserRequest, UserResponse };
