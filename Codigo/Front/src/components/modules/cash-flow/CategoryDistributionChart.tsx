import type { TooltipItem } from "chart.js";
import { Pie } from "react-chartjs-2";
import Card from "@/components/ui/Card";
import type { DashboardData } from "@/services/dashboardService";
import { categoryNames } from "./constants";

interface CategoryDistributionChartProps {
  dashboardData: DashboardData | null;
}

export default function CategoryDistributionChart({
  dashboardData,
}: CategoryDistributionChartProps) {
  const pieChartData = {
    labels: dashboardData
      ? Object.keys(dashboardData.PorcentagemPorCategoria || {}).map(
          (key) => categoryNames[key] || key,
        )
      : [],
    datasets: [
      {
        data: dashboardData
          ? Object.values(dashboardData.PorcentagemPorCategoria || {}).map(
              (item) => item.Porcentagem,
            )
          : [],
        backgroundColor: [
          "#FF6384", // VENDAS_CARTAO
          "#36A2EB", // SERVIÇOS_BANCARIOS
          "#FFCE56", // FORNECEDOR
          "#4BC0C0", // FAMÍLIA
          "#9966FF", // VENDAS_PIX
          "#FF9F40", // FUNCIONARIO
          "#C9CBCF", // FISCAL
        ],
        hoverBackgroundColor: [
          "#FF6384",
          "#36A2EB",
          "#FFCE56",
          "#4BC0C0",
          "#9966FF",
          "#FF9F40",
          "#C9CBCF",
        ],
      },
    ],
  };

  const pieOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: "bottom" as const,
        labels: {
          boxWidth: 12,
          padding: 10,
          font: {
            size: 11,
          },
        },
      },
      tooltip: {
        callbacks: {
          label: (context: TooltipItem<"pie">) => {
            const label = context.label || "";
            const percentage = context.parsed;
            const _datasetIndex = context.datasetIndex;
            const dataIndex = context.dataIndex;

            const categoryKey = Object.keys(
              dashboardData?.PorcentagemPorCategoria || {},
            )[dataIndex];
            const valor =
              dashboardData?.PorcentagemPorCategoria?.[categoryKey]?.Valor || 0;

            return [
              `${label}: ${percentage.toFixed(2)}%`,
              `R$ ${valor.toLocaleString("pt-BR", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}`,
            ];
          },
        },
      },
    },
    layout: {
      padding: {
        top: 10,
        bottom: 10,
        left: 10,
        right: 10,
      },
    },
  };

  return (
    <Card title="Distribuição por Categoria">
      <div className="h-72 sm:h-80 w-full flex items-center justify-center overflow-hidden">
        <div className="w-full h-full max-w-full">
          {dashboardData?.PorcentagemPorCategoria &&
          Object.keys(dashboardData.PorcentagemPorCategoria).length > 0 ? (
            <Pie
              data={pieChartData}
              options={pieOptions}
              style={{ width: "100%", height: "100%" }}
            />
          ) : (
            <div className="flex flex-col items-center justify-center h-full text-gray-500">
              <p>Nenhum dado de categorias disponível.</p>
              <p className="text-sm">
                Tente alterar o intervalo de datas no filtro.
              </p>
            </div>
          )}
        </div>
      </div>
    </Card>
  );
}
