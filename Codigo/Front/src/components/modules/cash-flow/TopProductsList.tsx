import { Package } from "lucide-react";
import Card from "@/components/ui/Card";
import type { DashboardData } from "@/services/dashboardService";
import { formatCurrency } from "@/utils/formatCurrency";

interface TopProductsListProps {
  dashboardData: DashboardData | null;
}

export default function TopProductsList({
  dashboardData,
}: TopProductsListProps) {
  const produtosEmAlta = dashboardData?.["Produtos em Alta"] || [];
  const hasProdutosEmAlta = produtosEmAlta.length > 0;

  return (
    <Card title="Produtos em Alta (Top 10)">
      <div className="space-y-2 max-h-60 sm:max-h-80 overflow-y-auto">
        {hasProdutosEmAlta ? (
          produtosEmAlta.map((produto, index) => (
            <div
              key={produto.Nome}
              className="flex items-center justify-between p-3 bg-gradient-to-r from-lime-50 to-green-50 rounded-lg hover:from-orange-100 hover:to-yellow-100 transition-colors border border-orange-200"
            >
              <div className="flex items-center space-x-3 flex-1 min-w-0">
                <span className="flex items-center justify-center w-8 h-8 bg-gradient-to-br from-orange-500 to-yellow-500 text-white rounded-full text-sm font-bold shadow-sm">
                  {index + 1}
                </span>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-sm text-gray-800 truncate">
                    {produto.Nome}
                  </p>
                  <p className="text-xs text-gray-600">
                    Quantidade: {produto.QuantidadeTotal} un.
                  </p>
                </div>
              </div>
              <div className="text-right ml-3">
                <p className="text-sm font-bold text-orange-600">
                  {formatCurrency(produto.ValorTotal)}
                </p>
              </div>
            </div>
          ))
        ) : (
          <div className="flex flex-col items-center justify-center h-64 text-gray-500">
            <Package className="w-12 h-12 mb-3 text-gray-400" />
            <p className="text-center">Nenhum produto em destaque</p>
            <p className="text-xs text-center mt-1">
              Aguarde o processamento dos dados de vendas
            </p>
          </div>
        )}
      </div>
    </Card>
  );
}
