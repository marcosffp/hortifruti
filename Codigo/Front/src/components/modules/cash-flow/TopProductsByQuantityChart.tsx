import type { TooltipItem } from "chart.js";
import { BarChart3 } from "lucide-react";
import { Bar } from "react-chartjs-2";
import Card from "@/components/ui/Card";
import type { DashboardData } from "@/services/dashboardService";
import { truncate } from "./chartHelpers";

interface TopProductsByQuantityChartProps {
  dashboardData: DashboardData | null;
}

export default function TopProductsByQuantityChart({
  dashboardData,
}: TopProductsByQuantityChartProps) {
  const top10ProdutosPorQuantidade =
    dashboardData?.Top10ProdutosPorQuantidade || [];
  const hasTop10ProdutosPorQuantidade = top10ProdutosPorQuantidade.length > 0;

  const topProductsByQuantityChartData = {
    labels: dashboardData
      ? dashboardData.Top10ProdutosPorQuantidade?.map((p) => p.Nome) || []
      : [],
    datasets: [
      {
        label: "Quantidade",
        data: dashboardData
          ? dashboardData.Top10ProdutosPorQuantidade?.map(
              (p) => p.QuantidadeTotal,
            ) || []
          : [],
        backgroundColor: "rgba(74, 222, 128, 0.8)",
        borderColor: "rgb(34, 197, 94)",
        borderWidth: 1,
      },
    ],
  };

  const quantityBarOptions = {
    indexAxis: "y" as const,
    responsive: true,
    maintainAspectRatio: false,
    layout: { padding: { left: 0, right: 0, top: 0, bottom: 0 } },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<"bar">) => {
            const value = context.parsed.x;
            return `${value} unidades`;
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
          callback: (value: number | string) => `${value} un.`,
        },
      },
      y: {
        ticks: {
          autoSkip: true,
          maxTicksLimit: 10,
          font: { size: 10 },
          callback: (_val: number | string, index: number) =>
            truncate(
              (topProductsByQuantityChartData.labels as string[])[index],
            ),
        },
      },
    },
  };

  return (
    <Card title="Top 10 Produtos Mais Vendidos (por Quantidade)">
      <div className="h-[360px] sm:h-96 w-full overflow-hidden">
        {hasTop10ProdutosPorQuantidade ? (
          <Bar
            data={topProductsByQuantityChartData}
            options={quantityBarOptions}
            style={{ width: "100%", height: "100%" }}
          />
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-gray-500">
            <BarChart3 className="w-12 h-12 mb-3 text-gray-400" />
            <p className="text-center">Nenhum dado de produtos disponível</p>
            <p className="text-xs text-center mt-1">
              Registre vendas para visualizar os produtos mais vendidos por
              quantidade
            </p>
          </div>
        )}
      </div>
    </Card>
  );
}
