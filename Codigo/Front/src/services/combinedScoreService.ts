import { API_BASE_URL } from "@/config/api";
import type {
  ClientLastGroupingType,
  CombinedScoreRequest,
  CombinedScoreType,
  GroupedProductType,
  PurchaseImageType,
} from "@/types/combinedScoreType";
import type { Page } from "@/types/PagesType";
import { getAuthHeaders } from "@/utils/httpUtils";

export const combinedScoreService = {
  async fetchCombinedScores(
    clientId?: number,
    page = 0,
    size = 20,
  ): Promise<Page<CombinedScoreType>> {
    let url = `${API_BASE_URL}/combined-scores?page=${page}&size=${size}`;
    if (clientId) url += `&clientId=${clientId}`;

    const response = await fetch(url, {
      headers: getAuthHeaders(),
      credentials: "include",
    });
    if (!response.ok) throw new Error("Erro ao buscar agrupamentos");
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

  async createCombinedScore(request: CombinedScoreRequest): Promise<string> {
    // Converte as datas para LocalDateTime (adiciona horário)
    const requestWithDateTime = {
      clientId: request.clientId,
      startDate: `${request.startDate}T00:00:00`,
      endDate: `${request.endDate}T23:59:59`,
      ...(request.confirmedAt && { confirmedAt: request.confirmedAt }),
    };

    const response = await fetch(`${API_BASE_URL}/combined-scores/create`, {
      method: "POST",
      headers: getAuthHeaders(),
      credentials: "include",
      body: JSON.stringify(requestWithDateTime),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(
        errorData?.message || errorData?.error || "Erro ao criar agrupamento",
      );
    }
    const data = await response.json();
    return data.message;
  },

  async cancelGrouping(id: number): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/combined-scores/${id}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
      credentials: "include",
    });
    if (!response.ok) throw new Error("Erro ao deletar agrupamento");
    const data = await response.json();
    return data.message;
  },

  async confirmPayment(id: number): Promise<string> {
    const response = await fetch(
      `${API_BASE_URL}/combined-scores/confirm-payment/${id}`,
      {
        method: "PATCH",
        headers: getAuthHeaders(),
        credentials: "include",
      },
    );
    if (!response.ok) throw new Error("Erro ao confirmar pagamento");
    const data = await response.json();
    return data.message;
  },

  async cancelPayment(id: number): Promise<string> {
    const response = await fetch(
      `${API_BASE_URL}/combined-scores/cancel-payment/${id}`,
      {
        method: "PATCH",
        headers: getAuthHeaders(),
        credentials: "include",
      },
    );
    if (!response.ok) throw new Error("Erro ao cancelar pagamento");
    const data = await response.json();
    return data.message;
  },

  async fetchGroupedProducts(
    combinedScoreId: number,
  ): Promise<GroupedProductType[]> {
    const response = await fetch(
      `${API_BASE_URL}/combined-scores/${combinedScoreId}/grouped-products`,
      { headers: getAuthHeaders(), credentials: "include" },
    );
    if (!response.ok) throw new Error("Erro ao buscar produtos agrupados");
    return await response.json();
  },

  async fetchImages(combinedScoreId: number): Promise<PurchaseImageType[]> {
    const response = await fetch(
      `${API_BASE_URL}/combined-scores/${combinedScoreId}/imagens`,
      { headers: getAuthHeaders(), credentials: "include" },
    );
    if (!response.ok) throw new Error("Erro ao buscar fotos do agrupamento");
    return await response.json();
  },

  async downloadPhotosPdf(combinedScoreId: number): Promise<Blob> {
    const response = await fetch(
      `${API_BASE_URL}/combined-scores/${combinedScoreId}/fotos/pdf`,
      { headers: getAuthHeaders(), credentials: "include" },
    );
    if (!response.ok)
      throw new Error("Erro ao baixar PDF de fotos do agrupamento");
    return await response.blob();
  },

  async fetchLastGroupingPerClient(): Promise<ClientLastGroupingType[]> {
    const response = await fetch(
      `${API_BASE_URL}/combined-scores/last-per-client`,
      {
        headers: getAuthHeaders(),
        credentials: "include",
      },
    );
    if (!response.ok) throw new Error("Erro ao buscar últimos agrupamentos");
    return await response.json();
  },

  async createWildcardBillet(clientId: number, value: number): Promise<number> {
    const response = await fetch(
      `${API_BASE_URL}/combined-scores/create-wildcard-billet`,
      {
        method: "POST",
        headers: getAuthHeaders(),
        credentials: "include",
        body: JSON.stringify({ clientId, value }),
      },
    );
    if (!response.ok) throw new Error("Erro ao criar boleto avulso");
    return await response.json();
  },
};
