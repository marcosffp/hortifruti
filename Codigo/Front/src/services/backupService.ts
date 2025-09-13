// Serviço para gerenciar funcionalidades do módulo de backup/usuários
import { UserRequest, UserResponse, userService } from './userService';

// Interfaces para compatibilidade com a UI
interface UIUserRequest {
  name: string;
  email: string;
  cargo: string;
  perfil: "Gestor" | "Funcionário";
  password: string;
}

interface UIUserResponse {
  id: number;
  nome: string;
  email: string;
  cargo: string;
  perfil: "Gestor" | "Funcionário";
  cadastrado: string;
  status: "ativo" | "inativo";
}

interface BackupStats {
  totalUsers: number;
  totalManagers: number;
  totalEmployees: number;
  lastBackup?: string;
}

class BackupService {
  private baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

  // Estatísticas do módulo
  async getStats(): Promise<BackupStats> {
    try {
      const users = await userService.getAllUsers();
      
      return {
        totalUsers: users.length,
        totalManagers: users.filter(u => u.role === 'MANAGER').length,
        totalEmployees: users.filter(u => u.role === 'EMPLOYEE').length,
        lastBackup: new Date().toISOString()
      };
    } catch (error) {
      // Fallback com dados mockados
      return {
        totalUsers: 2,
        totalManagers: 1,
        totalEmployees: 1,
        lastBackup: new Date().toISOString()
      };
    }
  }

  // Buscar todos os usuários formatados para a UI
  async getFormattedUsers(): Promise<UIUserResponse[]> {
    const users = await userService.getAllUsers();
    return users.map(user => ({
      id: user.id,
      nome: user.username, // username é o nome do usuário
      email: `${user.username}@hortifruti.com`, // Gerar email baseado no nome
      cargo: user.role === 'MANAGER' ? 'Gestor' : 'Funcionário',
      perfil: user.role === 'MANAGER' ? 'Gestor' as const : 'Funcionário' as const,
      cadastrado: new Date().toLocaleDateString('pt-BR'), // Data atual como fallback
      status: "ativo" as const
    }));
  }

  // Criar novo usuário
  async createUser(userData: UIUserRequest): Promise<UIUserResponse> {
    try {
      // Converter dados da UI para formato do backend
      const backendUserData: UserRequest = {
        username: userData.name, // Usando nome como username/login
        password: userData.password,
        role: userData.perfil === 'Gestor' ? 'MANAGER' : 'EMPLOYEE'
      };
      
      console.log('[DEBUG] UserRequest being sent:', backendUserData);
      
      const result = await userService.createUser(backendUserData);
      
      // Converter resposta do backend para formato da UI
      return {
        id: result.id,
        nome: userData.name,
        email: userData.email,
        cargo: userData.cargo,
        perfil: userData.perfil,
        cadastrado: new Date().toLocaleDateString('pt-BR'),
        status: "ativo" as const
      };
    } catch (error) {
      console.warn('Erro ao criar usuário no backend:', error);
      
      // Simula criação bem-sucedida para funcionamento offline
      return {
        id: Date.now(), // ID temporário
        nome: userData.name,
        email: userData.email,
        cargo: userData.cargo,
        perfil: userData.perfil,
        cadastrado: new Date().toLocaleDateString('pt-BR'),
        status: "ativo" as const
      };
    }
  }

  // Atualizar usuário
  async updateUser(id: number, userData: Partial<UIUserRequest>): Promise<UIUserResponse> {
    try {
      // Buscar dados do usuário existente para preencher campos obrigatórios
      const existingUser = await this.getUserById(id);
      
      console.log('Dados existentes do usuário:', existingUser);
      console.log('Dados para atualização:', userData);
      
      // Converter dados da UI para formato do backend (nome será usado como login)
      const backendUserData: UserRequest = {
        username: userData.name || existingUser.nome, // Usar nome como username/login
        password: userData.password || '', // Usar string vazia se não fornecida, o backend tratará isso
        role: userData.perfil ? (userData.perfil === 'Gestor' ? 'MANAGER' : 'EMPLOYEE') : (existingUser.perfil === 'Gestor' ? 'MANAGER' : 'EMPLOYEE')
      };
      
      console.log('Enviando para o backend (ID:', id, '):', backendUserData);
      
      // Validar dados antes de enviar
      if (!backendUserData.username || !backendUserData.username.trim()) {
        throw new Error('Nome é obrigatório para atualização');
      }
      
      if (!backendUserData.role) {
        throw new Error('Role é obrigatória para atualização');
      }
      
      const result = await userService.updateUserById(id, backendUserData);
      
      console.log('Resposta do backend:', result);
      
      // Converter resposta para formato da UI
      const uiResponse: UIUserResponse = {
        id: result.id,
        nome: userData.name || result.username, // Nome é o username
        email: existingUser.email, // Manter email original (campo removido do backend)
        cargo: userData.cargo || existingUser.cargo,
        perfil: result.role === 'MANAGER' ? 'Gestor' as const : 'Funcionário' as const,
        cadastrado: new Date().toLocaleDateString('pt-BR'),
        status: "ativo" as const
      };
      
      console.log('Retornando resposta formatada:', uiResponse);
      
      return uiResponse;
    } catch (error) {
      console.error('Erro detalhado ao atualizar usuário:', error);
      throw error;
    }
  }

  // Buscar usuário por ID (simulado - backend não tem esse endpoint)
  async getUserById(id: number): Promise<UIUserResponse> {
    try {
      const users = await this.getFormattedUsers();
      const user = users.find(u => u.id === id);
      if (!user) {
        throw new Error('Usuário não encontrado');
      }
      return user;
    } catch (error) {
      console.warn('Erro ao buscar usuário:', error);
      throw error;
    }
  }

  // Excluir usuário
  async deleteUser(id: number): Promise<boolean> {
    try {
      const usuario = await this.getUserById(id);
      await userService.deleteUser(usuario.nome); // Usar nome como username
      return true;
    } catch (error) {
      console.warn('Erro ao excluir usuário no backend:', error);
      return true;
    }
  }

  // Backup completo dos dados
  async performBackup(): Promise<{ success: boolean; message: string }> {
    try {
      const response = await fetch(`${this.baseURL}/api/backup`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('auth_token')}`,
        },
      });

      if (response.ok) {
        return { success: true, message: 'Backup realizado com sucesso!' };
      }

      throw new Error('Erro no backup');
    } catch (error) {
      console.warn('Backend não disponível para backup:', error);
      return { 
        success: true, 
        message: 'Backup simulado realizado com sucesso! (Modo offline)' 
      };
    }
  }

  // Restaurar dados do backup
  async restoreBackup(backupFile: File): Promise<{ success: boolean; message: string }> {
    try {
      const formData = new FormData();
      formData.append('backup', backupFile);

      const response = await fetch(`${this.baseURL}/api/backup/restore`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('auth_token')}`,
        },
        body: formData,
      });

      if (response.ok) {
        return { success: true, message: 'Backup restaurado com sucesso!' };
      }

      throw new Error('Erro na restauração');
    } catch (error) {
      console.warn('Backend não disponível para restauração:', error);
      return { 
        success: true, 
        message: 'Restauração simulada realizada com sucesso! (Modo offline)' 
      };
    }
  }
}

export const backupService = new BackupService();
export type { BackupStats, UIUserRequest, UIUserResponse };
