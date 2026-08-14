import Card from "@/components/ui/Card";
import type { DashboardData } from "@/services/dashboardService";
import { categoryNames } from "./constants";

interface CategoryRankingListProps {
  dashboardData: DashboardData | null;
}

export default function CategoryRankingList({
  dashboardData,
}: CategoryRankingListProps) {
  return (
    <Card title="Ranking de Gastos por Categoria">
      <div className="space-y-2 max-h-60 sm:max-h-80 overflow-y-auto">
        {dashboardData?.RankingCategoriasGastos &&
        dashboardData.RankingCategoriasGastos.length > 0 ? (
          dashboardData.RankingCategoriasGastos.map((item) => (
            <div
              key={item.Categoria}
              className="flex items-center justify-between p-3 bg-gradient-to-r from-lime-50 to-green-50 rounded-lg hover:from-orange-100 hover:to-yellow-100 transition-colors border border-orange-200"
            >
              <div className="flex items-center space-x-3">
                <span className="flex items-center justify-center w-8 h-8 bg-gradient-to-br from-orange-500 to-yellow-500 text-white rounded-full text-sm font-bold shadow-sm">
                  {item.Rank}
                </span>
                <span className="font-medium text-sm">
                  {categoryNames[item.Categoria] || item.Categoria}
                </span>
              </div>
              <span className="text-sm font-bold text-red-600">
                R${" "}
                {item.Valor.toLocaleString("pt-BR", {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </span>
            </div>
          ))
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-gray-500">
            <p>Nenhum dado de ranking disponível.</p>
            <p className="text-sm">
              Tente alterar o campo <strong>Mês</strong> no filtro de datas.
            </p>
          </div>
        )}
      </div>
    </Card>
  );
}
