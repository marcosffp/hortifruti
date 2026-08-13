"use client";

import { useCallback, useState } from "react";
import {
  type DashboardData,
  dashboardService,
  type GetDashboardDataParams,
} from "@/services/dashboardService";
import { getErrorMessage } from "@/types/errorType";

export function useDashboard() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const getDashboardData = useCallback(
    async (params: GetDashboardDataParams): Promise<DashboardData | null> => {
      const { signal } = params;
      setIsLoading(true);
      setError(null);
      try {
        const data = await dashboardService.getDashboardData(params);
        return data;
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") {
          return null;
        }
        setError(getErrorMessage(err));
        console.error("Erro no hook useDashboard:", err);
        return null;
      } finally {
        if (!signal?.aborted) {
          setIsLoading(false);
        }
      }
    },
    [],
  );

  return {
    isLoading,
    error,
    getDashboardData,
  };
}
