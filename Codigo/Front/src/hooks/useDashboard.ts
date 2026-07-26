"use client";

import { useCallback, useState } from "react";
import {
  type DashboardData,
  dashboardService,
} from "@/services/dashboardService";

export function useDashboard() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const getDashboardData = useCallback(
    async (
      startDate: string,
      endDate: string,
      month: number,
      year: number,
      signal?: AbortSignal,
    ): Promise<DashboardData | null> => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await dashboardService.getDashboardData(
          startDate,
          endDate,
          month,
          year,
          signal,
        );
        return data;
      } catch (err) {
        if (err instanceof DOMException && err.name === "AbortError") {
          return null;
        }
        const errorMessage =
          err instanceof Error
            ? err.message
            : "Erro ao buscar dados do dashboard.";
        setError(errorMessage);
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
