import { getAuthHeaders } from "@/utils/httpUtils";
import {
  CombinedScoreResponse,
  GroupedProductType,
  CombinedScoreRequest,
} from "@/types/combinedScoreType";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const combinedScoreService = {
  async fetchCombinedScores(clientId?: number, page = 0, size = 10): Promise<CombinedScoreResponse> {
    let url = `${API_BASE_URL}/combined-scores?page=${page}&size=${size}`;
    if (clientId) url += `&clientId=${clientId}`;
    
    const response = await fetch(url, { headers: getAuthHeaders() });
    if (!response.ok) throw new Error("Erro ao buscar agrupamentos");
    return await response.json();
  },

  async createCombinedScore(request: CombinedScoreRequest): Promise<string> {
    // Converte as datas para LocalDateTime (adiciona horário)
    const requestWithDateTime = {
      clientId: request.clientId,
      startDate: `${request.startDate}T00:00:00`,
      endDate: `${request.endDate}T23:59:59`,
    };

    const response = await fetch(`${API_BASE_URL}/combined-scores/create`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: JSON.stringify(requestWithDateTime),
    });
    if (!response.ok) throw new Error("Erro ao criar agrupamento");
    return await response.text();
  },

  async cancelGrouping(id: number): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/combined-scores/${id}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error("Erro ao deletar agrupamento");
    return await response.text();
  },

  async confirmPayment(id: number): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/combined-scores/confirm-payment/${id}`, {
      method: "PATCH",
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error("Erro ao confirmar pagamento");
    return await response.text();
  },

  async cancelPayment(id: number): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/combined-scores/cancel-payment/${id}`, {
      method: "PATCH",
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error("Erro ao cancelar pagamento");
    return await response.text();
  },

  async fetchGroupedProducts(combinedScoreId: number): Promise<GroupedProductType[]> {
    const response = await fetch(
      `${API_BASE_URL}/combined-scores/${combinedScoreId}/grouped-products`,
      { headers: getAuthHeaders() }
    );
    if (!response.ok) throw new Error("Erro ao buscar produtos agrupados");
    return await response.json();
  },
};