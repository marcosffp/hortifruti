import type { TooltipItem } from "chart.js";
import { TrendingUp } from "lucide-react";
import { Bar } from "react-chartjs-2";
import Card from "@/components/ui/Card";
import type { DashboardData } from "@/services/dashboardService";
import { formatCurrency } from "@/utils/formatCurrency";
import { formatAxisCurrency, truncate } from "./chartHelpers";

interface SalesFlowChartProps {
  dashboardData: DashboardData | null;
}

export default function SalesFlowChart({ dashboardData }: SalesFlowChartProps) {
  const fluxoVendasData = dashboardData?.["Fluxo de Vendas"] || {};
  const hasFluxoVendas = Object.keys(fluxoVendasData).length > 0;

  const salesFlowChartData = {
    labels: dashboardData
      ? Object.keys(dashboardData["Fluxo de Vendas"] || {})
      : [],
    datasets: [
      {
        label: "Vendas (R$)",
        data: dashboardData
          ? Object.values(dashboardData["Fluxo de Vendas"] || {})
          : [],
        backgroundColor: "rgba(59, 130, 246, 0.8)",
        borderColor: "rgb(59, 130, 246)",
        borderWidth: 1,
      },
    ],
  };

  const horizontalBarOptions = {
    indexAxis: "y" as const,
    responsive: true,
    maintainAspectRatio: false,
    layout: { padding: { left: 0, right: 0, top: 0, bottom: 0 } },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<"bar">) => {
            const value = context.parsed.x ?? 0;
            return formatCurrency(value);
          },
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        ticks: {
          autoSkip: true,
          maxTicksLimit: 6,
          font: { size: 10 },
          callback: formatAxisCurrency,
        },
      },
      y: {
        ticks: {
          autoSkip: true,
          maxTicksLimit: 10,
          font: { size: 10 },
          callback: (_val: number | string, index: number) =>
            truncate((salesFlowChartData.labels as string[])[index]),
        },
      },
    },
  };

  return (
    <Card title="Fluxo de Vendas (Semanal)">
      <div className="h-72 sm:h-80 w-full overflow-hidden">
        {hasFluxoVendas ? (
          <Bar
            data={salesFlowChartData}
            options={horizontalBarOptions}
            style={{ width: "100%", height: "100%" }}
          />
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-gray-500">
            <TrendingUp className="w-12 h-12 mb-3 text-gray-400" />
            <p className="text-center">Nenhum dado de vendas disponível</p>
            <p className="text-xs text-center mt-1">
              Crie agrupamentos de compras para visualizar o fluxo de vendas
            </p>
          </div>
        )}
      </div>
    </Card>
  );
}
