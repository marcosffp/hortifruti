import { API_BASE_URL } from "@/config/api";
import type { GroupedProductRequest } from "@/types/groupedType";
import type {
  InvoiceProductType,
  InvoiceProductUpdate,
  ManualPurchaseItemRequest,
  ManualPurchaseRequest,
  PurchaseType,
} from "@/types/purchaseType";
import { getAuthHeaders, getAuthHeadersForFormData } from "@/utils/httpUtils";

export const purchaseService = {
  async uploadPurchases(files: File[]): Promise<{ message: string }> {
    const formData = new FormData();
    formData.append("file", files[files.length - 1]);

    const response = await fetch(`${API_BASE_URL}/purchases/process`, {
      method: "POST",
      headers: getAuthHeadersForFormData(),
      credentials: "include",
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData?.error || "Erro ao enviar arquivos");
    }

    const data = await response.json();
    return { message: data.message };
  },

  async createManualPurchase(
    payload: ManualPurchaseRequest,
  ): Promise<PurchaseType> {
    const headers = {
      ...(getAuthHeaders() || {}),
      "Content-Type": "application/json",
    };
    const response = await fetch(`${API_BASE_URL}/purchases/manual`, {
      method: "POST",
      headers,
      credentials: "include",
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(
        errorData?.message || errorData?.error || "Erro ao criar compra",
      );
    }

    return await response.json();
  },

  async fetchClientProducts(
    clientId: number,
    startDate: string,
    endDate: string,
  ): Promise<GroupedProductRequest[]> {
    const sDate = startDate ? `${startDate}T00:00:00` : "";
    const eDate = endDate ? `${endDate}T23:59:59` : "";

    const response = await fetch(
      `${API_BASE_URL}/purchases/client/products?clientId=${clientId}&startDate=${encodeURIComponent(
        sDate,
      )}&endDate=${encodeURIComponent(eDate)}`,
      {
        headers: getAuthHeaders(),
        credentials: "include",
      },
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
        credentials: "include",
      },
    );
    if (!response.ok) throw new Error("Erro ao buscar arquivos de compra");
    const data = await response.json();
    // O backend serializa Page via PagedModel (VIA_DTO), aninhando os metadados em "page"
    return {
      content: data.content || [],
      totalElements: data.totalElements ?? data.page?.totalElements ?? 0,
      totalPages: data.totalPages ?? data.page?.totalPages ?? 0,
      number: data.number ?? data.page?.number ?? 0,
      size: data.size ?? data.page?.size ?? 0,
      first: data.first ?? (data.page?.number ?? 0) === 0,
      last:
        data.last ??
        (data.page?.number ?? 0) >= (data.page?.totalPages ?? 1) - 1,
    };
  },

  async deletePurchaseFile(fileId: number): Promise<{ message: string }> {
    const response = await fetch(`${API_BASE_URL}/purchases/${fileId}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
      credentials: "include",
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData?.error || "Erro ao deletar arquivo");
    }

    const data = await response.json();
    return { message: data.message };
  },

  async fetchInvoiceProducts(
    purchaseId: number,
  ): Promise<InvoiceProductType[]> {
    const response = await fetch(
      `${API_BASE_URL}/purchases/${purchaseId}/products`,
      { headers: getAuthHeaders(), credentials: "include" },
    );
    if (!response.ok) throw new Error("Erro ao buscar produtos da compra");
    return await response.json();
  },

  async addInvoiceProduct(
    purchaseId: number,
    item: ManualPurchaseItemRequest,
  ): Promise<InvoiceProductType> {
    const headers = {
      ...(getAuthHeaders() || {}),
      "Content-Type": "application/json",
    };
    const response = await fetch(
      `${API_BASE_URL}/purchases/${purchaseId}/products`,
      {
        method: "POST",
        headers,
        credentials: "include",
        body: JSON.stringify(item),
      },
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(
        errorData?.message ||
          errorData?.error ||
          `Erro ao adicionar produto (status ${response.status})`,
      );
    }

    return await response.json();
  },

  async updateInvoiceProduct(
    id: number,
    update: InvoiceProductUpdate,
  ): Promise<InvoiceProductType> {
    const headers = {
      ...(getAuthHeaders() || {}),
      "Content-Type": "application/json",
    };
    const response = await fetch(`${API_BASE_URL}/invoice-products/${id}`, {
      method: "PUT",
      headers,
      credentials: "include",
      body: JSON.stringify(update),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(
        errorData?.error ||
          `Erro ao atualizar produto (status ${response.status})`,
      );
    }

    return await response.json();
  },

  async deleteInvoiceProduct(productId: number): Promise<void> {
    const response = await fetch(
      `${API_BASE_URL}/invoice-products/${productId}`,
      {
        method: "DELETE",
        headers: getAuthHeaders(),
        credentials: "include",
      },
    );
    if (!response.ok) throw new Error("Erro ao deletar produto");
  },
};
