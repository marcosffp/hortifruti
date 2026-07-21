import { API_BASE_URL } from "@/config/api";
import type {
  InvoiceResponse,
  InvoiceResponseGet,
  InvoiceWithBilletResponse,
  InvoiceWithBilletResult,
  OpenInvoiceResponse,
} from "@/types/invoiceType";
import { getAuthHeaders } from "@/utils/httpUtils";

function base64ToBlob(base64: string, mimeType: string): Blob {
  const byteCharacters = atob(base64);
  const byteNumbers = new Array(byteCharacters.length);
  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
  }
  return new Blob([new Uint8Array(byteNumbers)], { type: mimeType });
}

export const invoiceService = {
  async generateInvoice(
    combinedScoreId: number,
    dadosAdicionais?: string,
  ): Promise<InvoiceResponse> {
    try {
      let url = `${API_BASE_URL}/invoices/issue/${combinedScoreId}`;

      if (dadosAdicionais?.trim()) {
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
        throw new Error(
          `Erro ao buscar informações da nota fiscal: ${response.status}`,
        );
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
      const response = await fetch(
        `${API_BASE_URL}/invoices/${ref}/xml/download`,
        {
          method: "GET",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

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

  async issueInvoiceAndBillet(
    combinedScoreId: number,
    dadosAdicionais?: string,
    dueDate?: string,
  ): Promise<InvoiceWithBilletResult> {
    try {
      const params = new URLSearchParams();
      if (dadosAdicionais?.trim()) {
        params.append("dadosAdicionais", dadosAdicionais);
      }
      if (dueDate) {
        params.append("dueDate", dueDate);
      }
      const queryString = params.toString();
      const url = `${API_BASE_URL}/invoices/issue-with-billet/${combinedScoreId}${queryString ? `?${queryString}` : ""}`;

      const response = await fetch(url, {
        method: "POST",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        let message = `Erro ao gerar nota fiscal e boleto: ${response.status}`;
        try {
          const errorBody = await response.json();
          if (errorBody?.message) message = errorBody.message;
        } catch {
          // corpo do erro não é JSON, mantém a mensagem padrão
        }
        throw new Error(message);
      }

      const result: InvoiceWithBilletResponse = await response.json();
      return {
        invoiceRef: result.invoiceRef,
        invoiceNumber: result.invoiceNumber,
        billetNumber: result.billetNumber,
        danfeBlob: base64ToBlob(result.danfeBase64, "application/pdf"),
        xmlBlob: base64ToBlob(result.xmlBase64, "application/xml"),
        billetBlob: base64ToBlob(result.billetBase64, "application/pdf"),
      };
    } catch (error) {
      console.error("Falha ao gerar nota fiscal e boleto vinculado:", error);
      throw error;
    }
  },

  async getOpenInvoiceOnly(): Promise<OpenInvoiceResponse[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/invoices/open`, {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(
          `Erro ao buscar notas fiscais em aberto: ${response.status}`,
        );
      }

      const result: OpenInvoiceResponse[] = await response.json();
      return result;
    } catch (error) {
      console.error("Falha ao buscar notas fiscais em aberto:", error);
      throw error;
    }
  },

  async cancelInvoice(ref: string, justificativa: string): Promise<string> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/invoices/${ref}/cancel?justificativa=${encodeURIComponent(justificativa)}`,
        {
          method: "DELETE",
          headers: getAuthHeaders(),
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(`Erro ao cancelar nota fiscal: ${response.status}`);
      }

      const result = await response.text();
      return result;
    } catch (error) {
      console.error("Falha ao cancelar nota fiscal:", error);
      throw error;
    }
  },
};
