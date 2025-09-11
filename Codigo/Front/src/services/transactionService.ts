import { getAuthHeaders } from "@/utils/httpUtils";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export interface TransactionResponse {
  id: number;
  document: string | null;
  history: string;
  category: string;
  transactionType: 'CREDITO' | 'DEBITO';
  transactionDate: string;
  amount: number;
  bank: string;
}

export interface TransactionRequest {
  document: string | null;
  history: string;
  category: string;
  transactionType: 'CREDITO' | 'DEBITO';
  transactionDate: string;
  amount: number;
  bank: string;
}

export interface PageResult<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export const transactionService = {
  async getTotalRevenueForCurrentMonth(): Promise<number> {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions/revenue`, {
        method: "GET",
        headers: getAuthHeaders(),
      });
      if (!response.ok) {
        throw new Error(`Erro ao buscar receita total: ${response.statusText}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Erro ao buscar receita total:", error);
      throw error;
    }
  },

  async getTotalExpensesForCurrentMonth(): Promise<number> {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions/expenses`, {
        method: "GET",
        headers: getAuthHeaders(),
      });
      if (!response.ok) {
        throw new Error(`Erro ao buscar despesas totais: ${response.statusText}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Erro ao buscar despesas totais:", error);
      throw error;
    }
  },

  async getTotalBalanceForCurrentMonth(): Promise<number> {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions/balance`, {
        method: "GET",
        headers: getAuthHeaders(),
      });
      if (!response.ok) {
        throw new Error(`Erro ao buscar saldo total: ${response.statusText}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Erro ao buscar saldo total:", error);
      throw error;
    }
  },

  async getAllTransactions(search?: string, type?: string, category?: string, page = 0, size = 20): Promise<PageResult<TransactionResponse>> {
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (type && type !== 'Todos os tipos') params.append('type', type === 'Entrada' ? 'CREDITO' : 'DEBITO');
    if (category && category !== 'Todas as categorias') params.append('category', category);
    params.append('page', page.toString());
    params.append('size', size.toString());

    const queryString = params.toString();
    const url = `${API_BASE_URL}/transactions${queryString ? `?${queryString}` : ''}`;

    const response = await fetch(url, {
      method: "GET",
      headers: getAuthHeaders(),
    });
    if (!response.ok) {
      throw new Error(`Erro ao buscar todas as transações: ${response.statusText}`);
    }
    const data = await response.json();
    return data; // data.content, data.totalPages, data.totalElements, etc.
  },

  async updateTransaction(id: number, transaction: TransactionRequest): Promise<TransactionResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...getAuthHeaders() },
        body: JSON.stringify(transaction),
      });
      if (!response.ok) {
        throw new Error(`Erro ao atualizar transação: ${response.statusText}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Erro ao atualizar transação:", error);
      throw error;
    }
  },

  async deleteTransaction(id: number): Promise<void> {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions/${id}`, {
        method: "DELETE",
        headers: getAuthHeaders(),
      });
      if (!response.ok) {
        throw new Error(`Erro ao deletar transação: ${response.statusText}`);
      }
    } catch (error) {
      console.error("Erro ao deletar transação:", error);
      throw error;
    }
  },

  async exportTransactionsAsExcel(): Promise<Blob> {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions/export`, {
        method: "POST",
        headers: getAuthHeaders(),
      });
      if (!response.ok) {
        throw new Error(`Erro ao exportar transações: ${response.statusText}`);
      }
      const blob = await response.blob();
      return blob;
    } catch (error) {
      console.error("Erro ao exportar transações:", error);
      throw error;
    }
  },

  async getAllCategories(): Promise<string[]> {
    const response = await fetch(`${API_BASE_URL}/transactions/categories`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error("Erro ao buscar categorias");
    return await response.json();
  }
};
