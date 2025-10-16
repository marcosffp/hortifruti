"use client";

import { useState } from "react";
import { dashboardService, DashboardData } from "@/services/dashboardService";

export function useDashboard() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const getDashboardData = async (
    startDate: string,
    endDate: string,
    month: number,
    year: number
  ): Promise<DashboardData | null> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await dashboardService.getDashboardData(startDate, endDate, month, year);
      return data;
    } catch (err: any) {
      const errorMessage = err.message || "Erro ao buscar dados do dashboard.";
      setError(errorMessage);
      console.error("Erro no hook useDashboard:", err);
      return null;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    isLoading,
    error,
    getDashboardData,
  };
}