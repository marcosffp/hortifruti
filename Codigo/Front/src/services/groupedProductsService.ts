import { GroupedScoreResponse, GroupedScoreType, GroupedProductRequest } from "@/types/groupedType";
import { getAuthHeaders } from "@/utils/httpUtils";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const groupedProductsService = {
    async fetchGroupedProducts(clientId?: number, page = 0, size = 10) {
        const url = `${API_BASE_URL}/grouped-products?clientId=${clientId}&page=${page}&size=${size}`;
        const response = await fetch(url, {
            headers: getAuthHeaders(),
        });

        if (!response.ok) throw new Error("Erro ao buscar produtos agrupados");

        const result: GroupedScoreResponse = await response.json();
        return result;
    },

    async confirmGrouping(clientId: number, groupedProducts: GroupedProductRequest[]) {
        if (!clientId || !groupedProducts.length) return;

        const url = `${API_BASE_URL}/grouped-products/confirm`;
        const response = await fetch(url, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ clientId, groupedProducts }),
        });

        if (!response.ok) throw new Error("Erro ao confirmar agrupamento de produtos");

        const result = await response.json();
        return result;
    },

    async cancelGrouping(groupId: number) {
        const url = `${API_BASE_URL}/grouped-products/${groupId}`;
        const response = await fetch(url, {
            method: "DELETE",
            headers: getAuthHeaders(),
        });

        if (!response.ok) throw new Error("Erro ao cancelar agrupamento de produtos");

        const result = await response.text();
        return result;
    },

    async confirmPayment(groupId: number) {
        const url = `${API_BASE_URL}/grouped-products/confirm-payment/${groupId}`;
        const response = await fetch(url, {
            method: "PATCH",
            headers: getAuthHeaders(),
        });

        if (!response.ok) throw new Error("Erro ao confirmar pagamento do agrupamento");

        const result = await response.text();
        return result;
    }
}