import { useCallback, useState } from "react";
import { groupedProductsService } from "@/services/groupedProductsService";
import { getErrorMessage } from "@/types/errorType";
import type {
  GroupedProductRequest,
  GroupedScoreType,
} from "@/types/groupedType";
import type { Page } from "@/types/PagesType";

export function useGroupedProducts() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchGroupedProducts = useCallback(
    async (
      clientId: number,
      page = 0,
      size = 10,
    ): Promise<Page<GroupedScoreType>> => {
      setIsLoading(true);
      setError(null);
      try {
        return await groupedProductsService.fetchGroupedProducts(
          clientId,
          page,
          size,
        );
      } catch (err) {
        setError(getErrorMessage(err));
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  const confirmGrouping = async (
    clientId: number,
    groupedProducts: GroupedProductRequest[],
  ) => {
    setIsLoading(true);
    setError(null);
    try {
      return await groupedProductsService.confirmGrouping(
        clientId,
        groupedProducts,
      );
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const confirmPayment = async (groupId: number) => {
    setIsLoading(true);
    setError(null);
    try {
      return await groupedProductsService.confirmPayment(groupId);
    } catch (err) {
      setError(getErrorMessage(err));
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
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    fetchGroupedProducts,
    confirmGrouping,
    confirmPayment,
    cancelGrouping,
    isLoading,
    error,
  };
}
