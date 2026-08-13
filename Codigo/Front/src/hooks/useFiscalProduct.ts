"use client";

import { useCallback, useState } from "react";
import { fiscalProductService } from "@/services/fiscalProductService";
import { getErrorMessage } from "@/types/errorType";

export function useFiscalProduct() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const getFiscalProducts = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      return await fiscalProductService.getFiscalProducts();
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { getFiscalProducts, isLoading, error };
}
