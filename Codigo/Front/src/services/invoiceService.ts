import { InvoiceResponse, InvoiceResponseGet } from "@/types/invoiceType";
import { getAuthHeaders } from "@/utils/httpUtils";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const invoiceService = {
  async generateInvoice(combinedScoreId: number): Promise<InvoiceResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/invoices/issue/${combinedScoreId}`, {
        method: "POST",
        headers: getAuthHeaders(),
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
