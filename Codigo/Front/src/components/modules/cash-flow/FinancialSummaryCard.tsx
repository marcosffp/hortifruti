import Card from "@/components/ui/Card";
import type { DashboardData } from "@/services/dashboardService";
import { formatCurrency } from "@/utils/formatCurrency";

interface FinancialSummaryCardProps {
  dashboardData: DashboardData | null;
}

export default function FinancialSummaryCard({
  dashboardData,
}: FinancialSummaryCardProps) {
  return (
    <Card title="Resumo Financeiro">
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 sm:gap-4">
        <div className="bg-green-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600">Total Receita</p>
          <p className="text-2xl font-bold text-green-600">
            {formatCurrency(dashboardData?.Totais?.TotalReceita || 0)}
          </p>
        </div>
        <div className="bg-red-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600">Total Custos</p>
          <p className="text-2xl font-bold text-red-600">
            {formatCurrency(Math.abs(dashboardData?.Totais?.TotalCusto || 0))}
          </p>
        </div>
        <div className="bg-blue-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600">Margem de Lucro</p>
          <p className="text-2xl font-bold text-blue-600">
            {(dashboardData?.Totais?.MargemLucro || 0).toFixed(2)}%
          </p>
        </div>
        <div className="bg-purple-50 p-4 rounded-lg">
          <p className="text-sm text-gray-600">Saldo</p>
          <p
            className={`text-2xl font-bold ${
              (dashboardData?.Totais?.TotalReceita || 0) -
                Math.abs(dashboardData?.Totais?.TotalCusto || 0) >=
              0
                ? "text-purple-600"
                : "text-red-600"
            }`}
          >
            {formatCurrency(
              (dashboardData?.Totais?.TotalReceita || 0) -
                Math.abs(dashboardData?.Totais?.TotalCusto || 0),
            )}
          </p>
        </div>
      </div>
    </Card>
  );
}
