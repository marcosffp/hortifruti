"use client";

import { useCallback, useState } from "react";
import { cepService } from "@/services/cepService";
import { getErrorMessage } from "@/types/errorType";

export function useCep() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const consultarCep = useCallback(async (cep: string) => {
    setIsLoading(true);
    setError(null);
    try {
      return await cepService.consultarCep(cep);
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { consultarCep, isLoading, error };
}
