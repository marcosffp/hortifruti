'use client';

import { getAuthHeaders } from '@/utils/httpUtils';
import { authService } from './authService';

// Tipos de dados para os clientes baseados no backend
export interface ClientRequest {
  clientName: string;
  email: string;
  phoneNumber: string;
  address: string;
  variablePrice: boolean;
}

export interface ClientResponse {
  id: number;
  clientName: string;
  email: string;
  phoneNumber: string;
  address: string;
  variablePrice: boolean;
}

// Definindo a URL base da API - pode ser ajustada conforme necessário
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Serviço para lidar com operações de cliente
export const clientService = {
  // Obter todos os clientes
  async getAllClients(): Promise<ClientResponse[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/clients`, {
        method: 'GET',
        headers: getAuthHeaders(),
        cache: 'no-store',
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar clientes: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Falha ao obter clientes:', error);
      throw error;
    }
  },

  // Obter cliente por ID
  async getClientById(id: number): Promise<ClientResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/clients/${id}`, {
        method: 'GET',
        headers: getAuthHeaders(),
        cache: 'no-store',
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar cliente: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`Falha ao obter cliente ${id}:`, error);
      throw error;
    }
  },

  // Criar novo cliente
  async createClient(clientData: ClientRequest): Promise<ClientResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/clients/register`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(clientData),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Erro ao criar cliente: ${response.status}`);
      }

      const data = await response.json();
      return data.client || data; // Tenta obter client da resposta, caso contrário retorna a resposta inteira
    } catch (error) {
      console.error('Falha ao criar cliente:', error);
      throw error;
    }
  },

  // Atualizar cliente existente
  async updateClient(id: number, clientData: ClientRequest): Promise<ClientResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/clients/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(clientData),
      });

      if (!response.ok) {
        throw new Error(`Erro ao atualizar cliente: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`Falha ao atualizar cliente ${id}:`, error);
      throw error;
    }
  },

  // Excluir cliente
  async deleteClient(id: number): Promise<void> {
    try {
      const response = await fetch(`${API_BASE_URL}/clients/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error(`Erro ao excluir cliente: ${response.status}`);
      }
    } catch (error) {
      console.error(`Falha ao excluir cliente ${id}:`, error);
      throw error;
    }
  },

  // Pesquisar clientes pelo nome
  async getClientByName(name: string): Promise<ClientResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/clients/name/${encodeURIComponent(name)}`, {
        method: 'GET',
        headers: getAuthHeaders(),
        cache: 'no-store',
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar cliente por nome: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`Falha ao buscar cliente pelo nome ${name}:`, error);
      throw error;
    }
  },
};
