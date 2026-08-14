import { Line } from "react-chartjs-2";
import Card from "@/components/ui/Card";
import type { DashboardData } from "@/services/dashboardService";
import { chartOptions } from "./chartHelpers";
import { monthNames, monthOrder } from "./constants";

interface CashFlowLineChartProps {
  dashboardData: DashboardData | null;
}

function buildLineChartData(dashboardData: DashboardData | null) {
  if (!dashboardData?.FluxoDeCaixa) {
    return {
      labels: [],
      datasets: [
        {
          label: "Receitas",
          data: [],
          borderColor: "rgb(34, 197, 94)",
          backgroundColor: "rgba(34, 197, 94, 0.2)",
          tension: 0.1,
        },
        {
          label: "Despesas",
          data: [],
          borderColor: "rgb(239, 68, 68)",
          backgroundColor: "rgba(239, 68, 68, 0.2)",
          tension: 0.1,
        },
      ],
    };
  }

  const sortedMonths = Object.keys(dashboardData.FluxoDeCaixa).sort(
    (a, b) => (monthOrder[a] || 0) - (monthOrder[b] || 0),
  );

  const labels = sortedMonths.map((month) => monthNames[month] || month);
  const receitas = sortedMonths.map(
    (month) => dashboardData.FluxoDeCaixa[month]?.Receitas || 0,
  );
  // Manter valores negativos (não usar Math.abs)
  const despesas = sortedMonths.map(
    (month) => dashboardData.FluxoDeCaixa[month]?.Despesas || 0,
  );

  return {
    labels,
    datasets: [
      {
        label: "Receitas",
        data: receitas,
        borderColor: "rgb(34, 197, 94)",
        backgroundColor: "rgba(34, 197, 94, 0.2)",
        tension: 0.1,
      },
      {
        label: "Despesas",
        data: despesas,
        borderColor: "rgb(239, 68, 68)",
        backgroundColor: "rgba(239, 68, 68, 0.2)",
        tension: 0.1,
      },
    ],
  };
}

export default function CashFlowLineChart({
  dashboardData,
}: CashFlowLineChartProps) {
  const lineChartData = buildLineChartData(dashboardData);

  return (
    <Card title="Fluxo de Caixa Mensal">
      <div className="h-56 sm:h-64 md:h-72 w-full overflow-hidden">
        {dashboardData?.FluxoDeCaixa &&
        Object.keys(dashboardData.FluxoDeCaixa).length > 0 ? (
          <Line
            data={lineChartData}
            options={chartOptions}
            style={{ width: "100%", height: "100%" }}
          />
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-gray-500">
            <p>Nenhum dado de fluxo de caixa disponível.</p>
            <p className="text-sm">
              Tente alterar o intervalo de datas no filtro.
            </p>
          </div>
        )}
      </div>
    </Card>
  );
}
