import { API_BASE_URL_WITH_API_PREFIX } from "@/config/api";
import { type UserRequest, userService } from "./userService";

// Interfaces para compatibilidade com a UI
interface UIUserRequest {
  name: string;
  cargo: string;
  perfil: "Gestor" | "Funcionário";
  password: string;
}

interface UIUserResponse {
  id: number;
  nome: string;
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

class UserAdminService {
  private baseURL = API_BASE_URL_WITH_API_PREFIX;

  async getStats(): Promise<BackupStats> {
    const users = await userService.getAllUsers();

    return {
      totalUsers: users.length,
      totalManagers: users.filter((u) => u.role === "MANAGER").length,
      totalEmployees: users.filter((u) => u.role === "EMPLOYEE").length,
      lastBackup: new Date().toISOString(),
    };
  }

  async getFormattedUsers(): Promise<UIUserResponse[]> {
    const users = await userService.getAllUsers();
    return users.map((user) => ({
      id: user.id,
      nome: user.username,
      cargo: user.position || "N/A",
      perfil:
        user.role === "MANAGER"
          ? ("Gestor" as const)
          : ("Funcionário" as const),
      cadastrado: new Date().toLocaleDateString("pt-BR"), // Data atual como fallback
      status: "ativo" as const,
    }));
  }

  async createUser(userData: UIUserRequest): Promise<UIUserResponse> {
    try {
      const backendUserData: UserRequest = {
        username: userData.name,
        password: userData.password,
        position: userData.cargo,
        role: userData.perfil === "Gestor" ? "MANAGER" : "EMPLOYEE",
      };

      const result = await userService.createUser(backendUserData);

      return {
        id: result.id,
        nome: userData.name,
        cargo: userData.cargo,
        perfil: userData.perfil,
        cadastrado: new Date().toLocaleDateString("pt-BR"),
        status: "ativo" as const,
      };
    } catch (error) {
      console.error("Erro ao criar usuário no backend:", error);
      throw error;
    }
  }

  async updateUser(
    id: number,
    userData: Partial<UIUserRequest>,
  ): Promise<UIUserResponse> {
    try {
      const existingUser = await this.getUserById(id);
      const backendUserData: UserRequest = {
        username: userData.name || existingUser.nome,
        password: userData.password || "",
        position: userData.cargo || existingUser.cargo,
        role: userData.perfil
          ? userData.perfil === "Gestor"
            ? "MANAGER"
            : "EMPLOYEE"
          : existingUser.perfil === "Gestor"
            ? "MANAGER"
            : "EMPLOYEE",
      };

      if (!backendUserData.username?.trim()) {
        throw new Error("Nome é obrigatório para atualização");
      }

      if (!backendUserData.role) {
        throw new Error("Role é obrigatória para atualização");
      }

      const result = await userService.updateUserById(id, backendUserData);

      const uiResponse: UIUserResponse = {
        id: result.id,
        nome: userData.name || result.username,
        cargo: userData.cargo || existingUser.cargo,
        perfil:
          result.role === "MANAGER"
            ? ("Gestor" as const)
            : ("Funcionário" as const),
        cadastrado: new Date().toLocaleDateString("pt-BR"),
        status: "ativo" as const,
      };

      return uiResponse;
    } catch (error) {
      console.error("Erro detalhado ao atualizar usuário:", error);
      throw error;
    }
  }

  // Simulado - backend não tem esse endpoint
  async getUserById(id: number): Promise<UIUserResponse> {
    try {
      const users = await this.getFormattedUsers();
      const user = users.find((u) => u.id === id);
      if (!user) {
        throw new Error("Usuário não encontrado");
      }
      return user;
    } catch (error) {
      console.warn("Erro ao buscar usuário:", error);
      throw error;
    }
  }

  async deleteUser(id: number): Promise<boolean> {
    try {
      const usuario = await this.getUserById(id);
      await userService.deleteUser(usuario.nome);
      return true;
    } catch (error) {
      console.error("Erro ao excluir usuário no backend:", error);
      throw error;
    }
  }

  async performBackup(): Promise<{ success: boolean; message: string }> {
    try {
      const response = await fetch(`${this.baseURL}/acesso`, {
        method: "POST",
        credentials: "include",
      });

      if (response.ok) {
        return { success: true, message: "Backup realizado com sucesso!" };
      }

      throw new Error("Erro no backup");
    } catch (error) {
      console.error("Erro ao realizar backup:", error);
      throw error;
    }
  }

  async restoreBackup(
    backupFile: File,
  ): Promise<{ success: boolean; message: string }> {
    try {
      const formData = new FormData();
      formData.append("backup", backupFile);

      const response = await fetch(`${this.baseURL}/acesso/restore`, {
        method: "POST",
        credentials: "include",
        body: formData,
      });

      if (response.ok) {
        return { success: true, message: "Backup restaurado com sucesso!" };
      }

      throw new Error("Erro na restauração");
    } catch (error) {
      console.error("Erro ao restaurar backup:", error);
      throw error;
    }
  }
}

export const userAdminService = new UserAdminService();
export type { BackupStats, UIUserRequest, UIUserResponse };
