import { API_BASE_URL } from "@/config/api";
import { getAuthHeaders } from "@/utils/httpUtils";

export interface TransactionResponse {
  id: number;
  document: string | null;
  history: string;
  category: string;
  transactionType: "CREDITO" | "DEBITO";
  transactionDate: string;
  amount: number;
  bank: string;
  codHistory?: string;
  batch?: string;
  sourceAgency?: string;
}

export interface TransactionRequest {
  document: string | null;
  history: string;
  category: string;
  transactionType: "CREDITO" | "DEBITO";
  transactionDate: string;
  amount: number;
  bank: string;
  codHistory: string;
  batch: string;
  sourceAgency: string;
}

export interface PageResult<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export const transactionService = {
  getDateRange(startDate?: string, endDate?: string) {
    if (startDate && endDate) {
      return { startDate, endDate };
    }

    // Se não foram fornecidas datas, usar o mês atual
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    return {
      startDate: firstDay.toISOString().split("T")[0], // formato YYYY-MM-DD
      endDate: lastDay.toISOString().split("T")[0],
    };
  },

  async getTotalRevenueForCurrentMonth(
    startDate?: string,
    endDate?: string,
  ): Promise<number> {
    try {
      const { startDate: defaultStartDate, endDate: defaultEndDate } =
        this.getDateRange(startDate, endDate);
      const response = await fetch(
        `${API_BASE_URL}/transactions/revenue?startDate=${defaultStartDate}&endDate=${defaultEndDate}`,
        {
          method: "GET",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );
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

  async getTotalExpensesForCurrentMonth(
    startDate?: string,
    endDate?: string,
  ): Promise<number> {
    try {
      const { startDate: defaultStartDate, endDate: defaultEndDate } =
        this.getDateRange(startDate, endDate);
      const response = await fetch(
        `${API_BASE_URL}/transactions/expenses?startDate=${defaultStartDate}&endDate=${defaultEndDate}`,
        {
          method: "GET",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );
      if (!response.ok) {
        throw new Error(
          `Erro ao buscar despesas totais: ${response.statusText}`,
        );
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Erro ao buscar despesas totais:", error);
      throw error;
    }
  },

  async getTotalBalanceForCurrentMonth(
    startDate?: string,
    endDate?: string,
  ): Promise<number> {
    try {
      const { startDate: defaultStartDate, endDate: defaultEndDate } =
        this.getDateRange(startDate, endDate);
      const response = await fetch(
        `${API_BASE_URL}/transactions/balance?startDate=${defaultStartDate}&endDate=${defaultEndDate}`,
        {
          method: "GET",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );
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

  async getAllTransactions(
    search?: string,
    type?: string,
    category?: string,
    page = 0,
    size = 20,
  ): Promise<PageResult<TransactionResponse>> {
    const params = new URLSearchParams();
    if (search) params.append("search", search);
    if (type && type !== "Todos os tipos")
      params.append("type", type === "Entrada" ? "CREDITO" : "DEBITO");
    if (category && category !== "Todas as categorias")
      params.append("category", category);
    params.append("page", page.toString());
    params.append("size", size.toString());

    const queryString = params.toString();
    const url = `${API_BASE_URL}/transactions${queryString ? `?${queryString}` : ""}`;

    const response = await fetch(url, {
      method: "GET",
      headers: getAuthHeaders(),
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(
        `Erro ao buscar todas as transações: ${response.statusText}`,
      );
    }

    const data = await response.json();

    const result: PageResult<TransactionResponse> = {
      content: data.content || [],
      totalPages: data.totalPages || data.page?.totalPages || 0,
      totalElements: data.totalElements || data.page?.totalElements || 0,
      size: data.size || data.page?.size || 0,
      number: data.number || data.page?.number || 0,
    };

    return result;
  },

  async updateTransaction(
    id: number,
    transaction: TransactionRequest,
  ): Promise<TransactionResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...getAuthHeaders() },
        credentials: "include",
        body: JSON.stringify(transaction),
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error("Resposta de erro do servidor:", errorText);
        throw new Error(
          `Erro ao atualizar transação: ${response.status} ${response.statusText}`,
        );
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
        credentials: "include",
      });
      if (!response.ok) {
        throw new Error(`Erro ao deletar transação: ${response.statusText}`);
      }
    } catch (error) {
      console.error("Erro ao deletar transação:", error);
      throw error;
    }
  },

  async exportTransactionsAsExcel(
    startDate?: string,
    endDate?: string,
  ): Promise<Blob> {
    try {
      const params = new URLSearchParams();
      if (startDate) params.append("startDate", startDate);
      if (endDate) params.append("endDate", endDate);
      const query = params.toString();

      const response = await fetch(
        `${API_BASE_URL}/transactions/export${query ? `?${query}` : ""}`,
        {
          method: "POST",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );
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

  async exportTransactionsComplete(): Promise<Blob> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/transactions/export-complete`,
        {
          method: "POST",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );
      if (!response.ok) {
        throw new Error(
          `Erro ao exportar relatório completo: ${response.statusText}`,
        );
      }
      const blob = await response.blob();
      return blob;
    } catch (error) {
      console.error("Erro ao exportar relatório completo:", error);
      throw error;
    }
  },

  async getAllCategories(): Promise<string[]> {
    const response = await fetch(`${API_BASE_URL}/transactions/categories`, {
      headers: getAuthHeaders(),
      credentials: "include",
    });
    if (!response.ok) throw new Error("Erro ao buscar categorias");
    return await response.json();
  },
};
