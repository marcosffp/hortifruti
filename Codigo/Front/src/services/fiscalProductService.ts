import { API_BASE_URL } from "@/config/api";
import type { FiscalProductType } from "@/types/purchaseType";
import { getAuthHeaders } from "@/utils/httpUtils";

export const fiscalProductService = {
  async getFiscalProducts(): Promise<FiscalProductType[]> {
    const response = await fetch(`${API_BASE_URL}/fiscal-products`, {
      headers: getAuthHeaders(),
      credentials: "include",
    });

    if (!response.ok) throw new Error("Erro ao buscar catálogo de produtos");
    return await response.json();
  },
};
