import { API_BASE_URL } from "@/config/api";
import { getAuthHeaders } from "@/utils/httpUtils";

export interface BankBalance {
  saldoDisponivel: number;
  detalhado: boolean;
  consultadoEm: string;
}

export const bankBalanceService = {
  async getSaldoDisponivel(): Promise<BankBalance> {
    const response = await fetch(`${API_BASE_URL}/finance/bank-balance`, {
      method: "GET",
      headers: getAuthHeaders(),
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(`Erro ao buscar saldo bancário: ${response.statusText}`);
    }

    return response.json();
  },
};
