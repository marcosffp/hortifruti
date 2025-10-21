import { BilletResponse } from "@/types/billetType";
import { getAuthHeaders } from "@/utils/httpUtils";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const billetService = {
  async generateBillet(combinedScoreId: number, number: string): Promise<Blob> {
    try {
      const response = await fetch(`${API_BASE_URL}/billet/generate/${combinedScoreId}?number=${encodeURIComponent(number)}`, {
        method: "GET",
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error(`Erro ao gerar boleto: ${response.status}`);
      }

      const result = await response.blob();
      return result;
    } catch (error) {
      console.error("Falha ao gerar boleto:", error);
      throw error;
    }
  },

  async fetchBilletPdf(combinedScoreId: number): Promise<Blob> {
    try {
      const response = await fetch(`${API_BASE_URL}/billet/${combinedScoreId}`, {
        method: "GET",
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar PDF do boleto: ${response.status}`);
      }

      const result = await response.blob();
      return result;
    } catch (error) {
      console.error("Falha ao buscar PDF do boleto:", error);
      throw error;
    }
  },

  async getClientBillets(clientId: number): Promise<BilletResponse[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/billet/client/${clientId}`, {
        method: "GET",
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar boletos do cliente: ${response.status}`);
      }

      const result: BilletResponse[] = await response.json();
      return result;
    } catch (error) {
      console.error("Falha ao buscar boletos do cliente:", error);
      throw error;
    }
  },

  async issueCopy(yourNumber: string, ourNumber: string): Promise<Blob> {
    try {
      const response = await fetch(`${API_BASE_URL}/billet/issue-copy/${ourNumber}/${yourNumber}`, {
        method: "GET",
        headers: getAuthHeaders(),
      });

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

  async cancelBillet(ourNumber: string): Promise<string> {
    try {
      const response = await fetch(`${API_BASE_URL}/billet/cancel/${ourNumber}`, {
        method: "POST",
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error(`Erro ao cancelar boleto: ${response.status}`);
      }

      const result = await response.text();
      return result;
    } catch (error) {
      console.error("Falha ao cancelar boleto:", error);
      throw error;
    }
  }
}