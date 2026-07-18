import { InvoiceResponse, InvoiceResponseGet } from "@/types/invoiceType";
import { getAuthHeaders } from "@/utils/httpUtils";
import { API_BASE_URL } from "@/config/api";

export const invoiceService = {
  async generateInvoice(combinedScoreId: number, dadosAdicionais?: string): Promise<InvoiceResponse> {
    try {
      let url = `${API_BASE_URL}/invoices/issue/${combinedScoreId}`;
      
      if (dadosAdicionais && dadosAdicionais.trim()) {
        url += `?dadosAdicionais=${encodeURIComponent(dadosAdicionais)}`;
      }

      const response = await fetch(url, {
        method: "POST",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Erro ao gerar nota fiscal: ${response.status}`);
      }

      const result: InvoiceResponse = await response.json();
      return result;
    } catch (error) {
      console.error("Falha ao gerar nota fiscal:", error);
      throw error;
    }
  },

  async fetchInvoiceInfo(ref: string): Promise<InvoiceResponseGet> {
    try {
      const response = await fetch(`${API_BASE_URL}/invoices/consulta/${ref}`, {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar informações da nota fiscal: ${response.status}`);
      }

      const result: InvoiceResponseGet = await response.json();
      return result;
    } catch (error) {
      console.error("Falha ao buscar informações da nota fiscal:", error);
      throw error;
    }
  },

  async downloadDanfe(ref: string): Promise<Blob> {
    try {
      const response = await fetch(`${API_BASE_URL}/invoices/${ref}/danfe`, {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Erro ao baixar DANFE: ${response.status}`);
      }

      const result = await response.blob();
      return result;
    } catch (error) {
      console.error("Falha ao baixar DANFE:", error);
      throw error;
    }
  },

  async downloadXml(ref: string): Promise<Blob> {
    try {
      const response = await fetch(`${API_BASE_URL}/invoices/${ref}/xml/download`, {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Erro ao baixar XML: ${response.status}`);
      }

      const result = await response.blob();
      return result;
    } catch (error) {
      console.error("Falha ao baixar XML:", error);
      throw error;
    }
  },

  async cancelInvoice(ref: string, justificativa: string): Promise<string> {
    try {
      const response = await fetch(`${API_BASE_URL}/invoices/${ref}/cancel?justificativa=${encodeURIComponent(justificativa)}`, {
        method: "DELETE",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Erro ao cancelar nota fiscal: ${response.status}`);
      }

      const result = await response.text();
      return result;
    } catch (error) {
      console.error("Falha ao cancelar nota fiscal:", error);
      throw error;
    }
  }
}
