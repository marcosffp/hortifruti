import { API_BASE_URL } from "@/config/api";
import type {
  BilletFilters,
  BilletResponse,
  OpenBilletResponse,
} from "@/types/billetType";
import { getAuthHeaders } from "@/utils/httpUtils";

export const billetService = {
  async generateBillet(
    combinedScoreId: number,
    number: string,
    dueDate?: string,
  ): Promise<Blob> {
    try {
      let url = `${API_BASE_URL}/billet/generate/${combinedScoreId}?number=${encodeURIComponent(number)}`;

      if (dueDate) {
        url += `&dueDate=${encodeURIComponent(dueDate)}`;
      }

      const response = await fetch(url, {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        let message = `Erro ao gerar boleto: ${response.status}`;
        try {
          const errorText = await response.text();
          if (errorText?.trim()) message = errorText;
        } catch {
          // corpo do erro indisponível, mantém a mensagem padrão
        }
        throw new Error(message);
      }

      const result = await response.blob();
      return result;
    } catch (error) {
      console.error("Falha ao gerar boleto:", error);
      throw error;
    }
  },

  async fetchBilletInfo(combinedScoreId: number): Promise<BilletResponse> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/billet/${combinedScoreId}`,
        {
          method: "GET",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(
          `Erro ao buscar informações do boleto: ${response.status}`,
        );
      }

      const result: BilletResponse = await response.json();
      return result;
    } catch (error) {
      console.error("Falha ao buscar informações do boleto:", error);
      throw error;
    }
  },

  async getClientBillets(
    clientId: number,
    filters?: BilletFilters,
  ): Promise<BilletResponse[]> {
    try {
      const params = new URLSearchParams();
      if (filters?.codigoSituacao)
        params.append("codigoSituacao", String(filters.codigoSituacao));
      if (filters?.dataInicio) params.append("dataInicio", filters.dataInicio);
      if (filters?.dataFim) params.append("dataFim", filters.dataFim);
      const queryString = params.toString();

      const response = await fetch(
        `${API_BASE_URL}/billet/client/${clientId}${queryString ? `?${queryString}` : ""}`,
        {
          method: "GET",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(
          `Erro ao buscar boletos do cliente: ${response.status}`,
        );
      }

      const result: BilletResponse[] = await response.json();
      return result;
    } catch (error) {
      console.error("Falha ao buscar boletos do cliente:", error);
      throw error;
    }
  },

  async getOpenBillets(): Promise<OpenBilletResponse[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/billet/open`, {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar boletos em aberto: ${response.status}`);
      }

      const result: OpenBilletResponse[] = await response.json();
      return result;
    } catch (error) {
      console.error("Falha ao buscar boletos em aberto:", error);
      throw error;
    }
  },

  async issueCopy(combinedScoreId: number): Promise<Blob> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/billet/issue-copy/${combinedScoreId}`,
        {
          method: "GET",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(`Erro ao emitir 2ª via do boleto: ${response.status}`);
      }

      const result = await response.blob();
      return result;
    } catch (error) {
      console.error("Falha ao emitir 2ª via do boleto:", error);
      throw error;
    }
  },

  async downloadStoredBillet(combinedScoreId: number): Promise<Blob> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/billet/${combinedScoreId}/file`,
        {
          method: "GET",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(`Erro ao baixar boleto armazenado: ${response.status}`);
      }

      const result = await response.blob();
      return result;
    } catch (error) {
      console.error("Falha ao baixar boleto armazenado:", error);
      throw error;
    }
  },

  async cancelBillet(combinedScoreId: number): Promise<string> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/billet/cancel/${combinedScoreId}`,
        {
          method: "POST",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(`Erro ao cancelar boleto: ${response.status}`);
      }

      const result = await response.text();
      return result;
    } catch (error) {
      console.error("Falha ao cancelar boleto:", error);
      throw error;
    }
  },

  async cancelBilletByNumber(nossoNumero: string): Promise<string> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/billet/cancel-by-number?nossoNumero=${encodeURIComponent(nossoNumero)}`,
        {
          method: "POST",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

      const result = await response.text();
      if (!response.ok) {
        throw new Error(
          result || `Erro ao cancelar boleto: ${response.status}`,
        );
      }

      return result;
    } catch (error) {
      console.error("Falha ao cancelar boleto avulso:", error);
      throw error;
    }
  },

  async markBilletAsPaid(combinedScoreId: number): Promise<string> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/billet/mark-paid/${combinedScoreId}`,
        {
          method: "PATCH",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

      const result = await response.text();
      if (!response.ok) {
        throw new Error(
          result || `Erro ao confirmar pagamento: ${response.status}`,
        );
      }

      return result;
    } catch (error) {
      console.error("Falha ao confirmar pagamento do boleto:", error);
      throw error;
    }
  },
};
