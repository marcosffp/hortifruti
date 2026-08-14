import { Bar } from "react-chartjs-2";
import Card from "@/components/ui/Card";
import type { DashboardData } from "@/services/dashboardService";
import { chartOptions } from "./chartHelpers";

interface RevenueByTypeChartProps {
  dashboardData: DashboardData | null;
}

export default function RevenueByTypeChart({
  dashboardData,
}: RevenueByTypeChartProps) {
  const barChartData = {
    labels: ["Vendas Cartão", "Vendas PIX"],
    datasets: [
      {
        label: "Receitas (R$)",
        data: dashboardData
          ? [
              dashboardData.ReceitasPorTipo?.VendasCartao || 0,
              dashboardData.ReceitasPorTipo?.VendasPix || 0,
            ]
          : [0, 0],
        backgroundColor: ["rgba(34, 197, 94, 0.8)", "rgba(59, 130, 246, 0.8)"],
        borderColor: ["rgb(34, 197, 94)", "rgb(59, 130, 246)"],
        borderWidth: 1,
      },
    ],
  };

  return (
    <Card title="Receitas por Tipo de Venda">
      <div className="h-56 sm:h-64 md:h-72 w-full overflow-hidden">
        {dashboardData?.ReceitasPorTipo ? (
          <Bar
            data={barChartData}
            options={chartOptions}
            style={{ width: "100%", height: "100%" }}
          />
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-gray-500">
            <p>Nenhum dado de receitas disponível.</p>
            <p className="text-sm">
              Tente alterar o intervalo de datas no filtro.
            </p>
          </div>
        )}
      </div>
    </Card>
  );
}
