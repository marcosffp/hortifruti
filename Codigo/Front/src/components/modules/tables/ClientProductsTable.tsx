import { useEffect, useState, useRef } from "react";
import SkeletonTableLoading from "@/components/ui/SkeletonTableLoading";
import { groupedProductsService } from "@/services/groupedProductsService";
import { purchaseService } from "@/services/purchaseService";
import { GroupedProductRequest } from "@/types/groupedType";
import { showError, showSuccess } from "@/services/notificationService";

interface ClientProductsTableProps {
  clientId: number | undefined;
  refreshKey?: number;
}

export default function ClientProductsTable({ clientId, refreshKey }: ClientProductsTableProps) {
  const [groupBy, setGroupBy] = useState<'week' | 'month' | 'custom'>('custom');
  const [startDate, setStartDate] = useState(() => {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    return firstDay.toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState(() => {
    const now = new Date();
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    return lastDay.toISOString().split('T')[0];
  });

  const [products, setProducts] = useState<GroupedProductRequest[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Debounce refs
  const debounceTimer = useRef<NodeJS.Timeout | null>(null);

  function getWeekInterval() {
    const today = new Date();
    let lastMonday = new Date(today);
    lastMonday.setDate(today.getDate() - ((today.getDay() + 6) % 7) - 7);
    lastMonday.setHours(0, 0, 0, 0);

    let lastSaturday = new Date(lastMonday);
    lastSaturday.setDate(lastMonday.getDate() + 6);
    lastSaturday.setHours(23, 59, 59, 999);

    return {
      start: lastMonday.toISOString().split('T')[0],
      end: lastSaturday.toISOString().split('T')[0],
    };
  }

  function getLastMonthInterval() {
    const today = new Date();
    const year = today.getMonth() === 0 ? today.getFullYear() - 1 : today.getFullYear();
    const month = today.getMonth() === 0 ? 11 : today.getMonth() - 1;

    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    return {
      start: firstDay.toISOString().split('T')[0],
      end: lastDay.toISOString().split('T')[0],
    };
  }

  useEffect(() => {
    if (groupBy === 'week') {
      const { start, end } = getWeekInterval();
      setStartDate(start);
      setEndDate(end);
    } else if (groupBy === 'month') {
      const { start, end } = getLastMonthInterval();
      setStartDate(start);
      setEndDate(end);
    }
    // Se for custom, não altera nada
  }, [groupBy]);

  // Debounced fetch
  useEffect(() => {
    if (!clientId) return;

    if (debounceTimer.current) clearTimeout(debounceTimer.current);

    debounceTimer.current = setTimeout(() => {
      const fetchProducts = async () => {
        setIsLoading(true);
        setError(null);
        try {
          const products = await purchaseService.fetchClientProducts(clientId, startDate, endDate);
          setProducts(products);
        } catch (err: any) {
          setError(err.message || "Erro ao buscar produtos do cliente");
        } finally {
          setIsLoading(false);
        }
      };

      fetchProducts();
    }, 600);

    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [clientId, startDate, endDate, refreshKey]);

  const handleConfirmGrouping = () => {
    groupedProductsService.confirmGrouping(clientId!, products)
      .then(() => {
        showSuccess("Agrupamento confirmado com sucesso!");
      })
      .catch((err) => {
        console.error(err);
        showError(err.message || "Erro ao confirmar agrupamento.");
      });
  }

  // Mapeamento das colunas para display
  const columnLabels: Record<keyof GroupedProductRequest, string> = {
    code: "Código",
    name: "Produto",
    quantity: "Quantidade",
    price: "Preço",
    totalValue: "Valor Total"
  };

  // Filtro de datas
  const renderDateFilter = () => (
    <div className="mb-4 p-4 bg-white">
      <div className="flex flex-wrap *:flex-grow items-end gap-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Tipo de Agrupamento
          </label>
          <select
            className="px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            value={groupBy}
            onChange={(e) => setGroupBy(e.target.value as 'week' | 'month' | 'custom')}
          >
            <option value="custom">Intervalo Personalizado</option>
            <option value="week">Semanal</option>
            <option value="month">Mensal</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Data Inicial
          </label>
          <input
            type="date"
            value={startDate}
            disabled={groupBy !== 'custom'}
            onChange={(e) => setStartDate(e.target.value)}
            className={`px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 ${groupBy !== 'custom' ? 'bg-gray-100 cursor-not-allowed' : ''}`}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Data Final
          </label>
          <input
            type="date"
            value={endDate}
            disabled={groupBy !== 'custom'}
            onChange={(e) => setEndDate(e.target.value)}
            className={`px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 ${groupBy !== 'custom' ? 'bg-gray-100 cursor-not-allowed' : ''}`}
          />
        </div>
        <div>
          <button
            onClick={handleConfirmGrouping}
            className={`px-4 w-full py-2 bg-[var(--primary-light)] text-white rounded-lg hover:bg-[var(--primary-dark)] cursor-pointer transition-colors ${products.length === 0 ? 'opacity-50 cursor-not-allowed hover:bg-[var(--primary-light)]' : ''}`}
            disabled={products.length === 0}
          >
            Confirmar Agrupamento
          </button>
        </div>
      </div>
    </div>
  );

  // Skeleton loading
  if (isLoading || !clientId) {
    return <SkeletonTableLoading title="Produtos do Cliente" cols={["Código", "Produto", "Preço", "Quantidade", "Valor Total"]} />;
  }

  if (error) {
    return (
      <div>
        <div className="bg-red-100 border border-red-300 rounded-lg p-4 text-center mt-6">
          {error}
        </div>
      </div>
    );
  }

  if (!products.length) {
    return (
      <div className="text-gray-800">
        <h2 className="text-lg font-semibold text-gray-800">Produtos do Cliente</h2>
        {renderDateFilter()}
        <p className="text-center text-gray-500">Nenhum produto encontrado para este cliente no período selecionado.</p>
      </div>
    );
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-800">Produtos do Cliente</h2>
      {renderDateFilter()}
      <table className="min-w-full border border-gray-200">
        <thead>
          <tr>
            {Object.keys(columnLabels).map((col) => (
              <th
                key={col}
                className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700"
              >
                {columnLabels[col as keyof GroupedProductRequest]}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {products.map((prod, idx) => (
            <tr key={prod.code || idx} className="hover:bg-gray-50">
              {Object.keys(columnLabels).map((col) => {
                const key = col as keyof GroupedProductRequest;
                const value = prod[key];
                
                return (
                  <td
                    key={col}
                    className="px-4 py-2 border-b border-gray-100 text-sm text-gray-800 whitespace-nowrap"
                  >
                    {(key === "totalValue" || key === "price")
                      ? `R$ ${Number(value).toLocaleString("pt-BR", {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}`
                      : value}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}