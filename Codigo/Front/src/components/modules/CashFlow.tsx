"use client";

import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip,
} from "chart.js";
import { useCallback, useEffect, useState } from "react";
import CashFlowFilters from "@/components/modules/cash-flow/CashFlowFilters";
import CashFlowLineChart from "@/components/modules/cash-flow/CashFlowLineChart";
import CategoryDistributionChart from "@/components/modules/cash-flow/CategoryDistributionChart";
import CategoryRankingList from "@/components/modules/cash-flow/CategoryRankingList";
import FinancialSummaryCard from "@/components/modules/cash-flow/FinancialSummaryCard";
import RevenueByTypeChart from "@/components/modules/cash-flow/RevenueByTypeChart";
import SalesFlowChart from "@/components/modules/cash-flow/SalesFlowChart";
import TopProductsByQuantityChart from "@/components/modules/cash-flow/TopProductsByQuantityChart";
import TopProductsList from "@/components/modules/cash-flow/TopProductsList";
import Loading from "@/components/ui/Loading";
import { useDashboard } from "@/hooks/useDashboard";
import type { DashboardData } from "@/services/dashboardService";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
);

type CashFlowProps = {
  startDate: string;
  endDate: string;
  setStartDate: React.Dispatch<React.SetStateAction<string>>;
  setEndDate: React.Dispatch<React.SetStateAction<string>>;
};

export default function CashFlow({
  startDate,
  endDate,
  setStartDate,
  setEndDate,
}: CashFlowProps) {
  const { isLoading, error, getDashboardData } = useDashboard();
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(
    null,
  );

  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());

  // Rascunho dos filtros: só é aplicado (e dispara a busca) quando o usuário clica em "Aplicar Filtro",
  // evitando buscas parciais enquanto o usuário ainda está trocando mês/ano/datas
  const [draftMonth, setDraftMonth] = useState(selectedMonth);
  const [draftYear, setDraftYear] = useState(selectedYear);
  const [draftStartDate, setDraftStartDate] = useState(startDate);
  const [draftEndDate, setDraftEndDate] = useState(endDate);

  const hasPendingChanges =
    draftMonth !== selectedMonth ||
    draftYear !== selectedYear ||
    draftStartDate !== startDate ||
    draftEndDate !== endDate;

  // biome-ignore lint/correctness/useExhaustiveDependencies: getDashboardData is recreated on every render by useDashboard and is not part of the fetch identity
  const fetchDashboardData = useCallback(
    async (signal?: AbortSignal) => {
      const data = await getDashboardData({
        startDate,
        endDate,
        month: selectedMonth,
        year: selectedYear,
        signal,
      });
      if (data) {
        setDashboardData(data);
      }
    },
    [startDate, endDate, selectedMonth, selectedYear],
  );

  useEffect(() => {
    // Cancela a requisição se o usuário navegar para outra tela (ou trocar o
    // filtro) antes dela terminar, liberando a conexão em vez de deixá-la
    // disputando espaço com a navegação.
    const controller = new AbortController();
    fetchDashboardData(controller.signal);
    return () => controller.abort();
  }, [fetchDashboardData]);

  const applyFilters = () => {
    setSelectedMonth(draftMonth);
    setSelectedYear(draftYear);
    setStartDate(draftStartDate);
    setEndDate(draftEndDate);
  };

  if (isLoading) {
    return <Loading />;
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <p className="text-red-700">Erro ao carregar dados: {error}</p>
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:gap-6 w-full max-w-full overflow-x-hidden">
      <CashFlowFilters
        draftMonth={draftMonth}
        draftYear={draftYear}
        draftStartDate={draftStartDate}
        draftEndDate={draftEndDate}
        setDraftMonth={setDraftMonth}
        setDraftYear={setDraftYear}
        setDraftStartDate={setDraftStartDate}
        setDraftEndDate={setDraftEndDate}
        hasPendingChanges={hasPendingChanges}
        isLoading={isLoading}
        onApplyFilters={applyFilters}
      />

      <FinancialSummaryCard dashboardData={dashboardData} />

      {/* Gráficos Principais */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <CashFlowLineChart dashboardData={dashboardData} />
        <RevenueByTypeChart dashboardData={dashboardData} />
      </div>

      {/* Gráfico de Pizza e Ranking */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <CategoryDistributionChart dashboardData={dashboardData} />
        <CategoryRankingList dashboardData={dashboardData} />
      </div>

      {/* Fluxo de Vendas e Produtos em Alta */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <SalesFlowChart dashboardData={dashboardData} />
        <TopProductsList dashboardData={dashboardData} />
      </div>

      {/* Top 10 Produtos por Quantidade */}
      <TopProductsByQuantityChart dashboardData={dashboardData} />
    </div>
  );
}
