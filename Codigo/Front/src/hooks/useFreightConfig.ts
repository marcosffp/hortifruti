"use client";

import { useCallback, useState } from "react";
import { freightService } from "@/services/freightService";
import { getErrorMessage } from "@/types/errorType";
import type { FreightConfigDTO } from "@/types/freightType";

export function useFreightConfig() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const getFreightConfig = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      return await freightService.getFreightConfig();
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const updateFreightConfig = useCallback(
    async (config: Partial<FreightConfigDTO>) => {
      setIsLoading(true);
      setError(null);
      try {
        return await freightService.updateFreightConfig(config);
      } catch (err) {
        setError(getErrorMessage(err));
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  return { getFreightConfig, updateFreightConfig, isLoading, error };
}
