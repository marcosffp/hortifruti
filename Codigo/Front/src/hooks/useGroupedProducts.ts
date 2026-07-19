import { useCallback, useState } from "react";
import { groupedProductsService } from "@/services/groupedProductsService";
import type { GroupedScoreResponse } from "@/types/groupedType";

export function useGroupedProducts() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchGroupedProducts = useCallback(
    async (
      clientId: number,
      page = 0,
      size = 10,
    ): Promise<GroupedScoreResponse> => {
      setIsLoading(true);
      setError(null);
      try {
        return await groupedProductsService.fetchGroupedProducts(
          clientId,
          page,
          size,
        );
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : "Erro ao buscar produtos agrupados",
        );
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  const confirmPayment = async (groupId: number) => {
    setIsLoading(true);
    setError(null);
    try {
      return await groupedProductsService.confirmPayment(groupId);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Erro ao confirmar pagamento do agrupamento",
      );
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const cancelGrouping = async (groupId: number) => {
    setIsLoading(true);
    setError(null);
    try {
      return await groupedProductsService.cancelGrouping(groupId);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Erro ao cancelar agrupamento de produtos",
      );
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    fetchGroupedProducts,
    confirmPayment,
    cancelGrouping,
    isLoading,
    error,
  };
}
