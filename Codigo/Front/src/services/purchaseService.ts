import { GroupedProductRequest } from "@/types/groupedType";
import { InvoiceProductType, InvoiceProductUpdate, PurchaseResponse } from "@/types/purchaseType";
import { getAuthHeadersForFormData, getAuthHeaders } from "@/utils/httpUtils";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const purchaseService = {
  // Upload de notas fiscais
  async uploadPurchases(files: File[]): Promise<{ message: string }> {
    const formData = new FormData();
    formData.append("file", files[files.length - 1]);

    const response = await fetch(`${API_BASE_URL}/purchases/process`, {
      method: "POST",
      headers: getAuthHeadersForFormData(),
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData?.error || "Erro ao enviar arquivos");
    }

    const data = await response.json();
    return { message: data.message };
  },

  async fetchClientProducts(
    clientId: number,
    startDate: string,
    endDate: string
  ): Promise<GroupedProductRequest[]> {
    const sDate = startDate ? `${startDate}T00:00:00` : "";
    const eDate = endDate ? `${endDate}T23:59:59` : "";

    const response = await fetch(
      `${API_BASE_URL}/purchases/client/products?clientId=${clientId}&startDate=${encodeURIComponent(
        sDate
      )}&endDate=${encodeURIComponent(eDate)}`,
      {
        headers: getAuthHeaders(),
      }
    );
    if (!response.ok) throw new Error("Erro ao buscar produtos do cliente");
    const data = await response.json();
    // O backend retorna { products: [...] }
    return data.products || [];
  },

  async fetchPurchaseFiles(clientId: number, page = 0, size = 10) {
    const response = await fetch(
      `${API_BASE_URL}/purchases/client/${clientId}/ordered?page=${page}&size=${size}`,
      {
        headers: getAuthHeaders(),
      }
    );
    if (!response.ok) throw new Error("Erro ao buscar arquivos de compra");
    const data = await response.json();
    // O backend retorna um objeto Page<PurchaseResponse>
    return data;
  },

  async deletePurchaseFile(fileId: number): Promise<{ message: string }> {
    const response = await fetch(`${API_BASE_URL}/purchases/${fileId}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData?.error || "Erro ao deletar arquivo");
    }

    const data = await response.json();
    return { message: data.message };
  },

  async fetchInvoiceProducts(purchaseId: number): Promise<InvoiceProductType[]> {
    const response = await fetch(
      `${API_BASE_URL}/purchases/${purchaseId}/products`,
      { headers: getAuthHeaders() }
    );
    if (!response.ok) throw new Error("Erro ao buscar produtos da compra");
    return await response.json();
  },

  // ADICIONADO: implementação para atualizar um invoice product via API (PUT /invoice-products/{id})
  async updateInvoiceProduct(
    id: number,
    update: InvoiceProductUpdate
  ): Promise<InvoiceProductType> {
    const headers = { ...(getAuthHeaders() || {}), "Content-Type": "application/json" };
    const response = await fetch(`${API_BASE_URL}/invoice-products/${id}`, {
      method: "PUT",
      headers,
      body: JSON.stringify(update),
    });

    if (!response.ok) {
      // tentativa de obter mensagem de erro do backend
      const errorData = await response.json().catch(() => null);
      throw new Error(errorData?.error || `Erro ao atualizar produto (status ${response.status})`);
    }

    return await response.json();
  },

  async deleteInvoiceProduct(productId: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/invoice-products/${productId}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error("Erro ao deletar produto");
  }
};
