import { ProductInfo } from "@/types/productType";
import { getAuthHeaders } from "@/utils/httpUtils";
import { useEffect, useState, useRef } from "react";

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

  const [products, setProducts] = useState<ProductInfo[]>([]);
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
          const sDate = startDate ? `${startDate}T00:00:00` : "";
          const eDate = endDate ? `${endDate}T23:59:59` : "";

          const response = await fetch(
            `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/purchases/client/products?clientId=${clientId}&startDate=${encodeURIComponent(
              sDate
            )}&endDate=${encodeURIComponent(eDate)}`, {
              headers: getAuthHeaders()
            }
          );
          if (!response.ok) throw new Error("Erro ao buscar produtos do cliente");
          const data = await response.json();
          setProducts(data.products || []);
        } catch (err: any) {
          setError(err.message || "Erro ao buscar produtos do cliente");
        } finally {
          setIsLoading(false);
        }
      };

      fetchProducts();
    }, 600); // 600ms de espera

    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [clientId, startDate, endDate, refreshKey]);

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
            onClick={() => {
              const now = new Date();
              const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
              const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
              setStartDate(firstDay.toISOString().split('T')[0]);
              setEndDate(lastDay.toISOString().split('T')[0]);
            }}
            disabled={groupBy !== 'custom'}
            className={`px-4 w-full py-2 bg-[var(--primary-light)] text-white rounded-lg hover:bg-[var(--primary-dark)] cursor-pointer transition-colors ${groupBy !== 'custom' ? 'opacity-50 cursor-not-allowed hover:bg-[var(--primary-light)]' : ''}`}
          >
            Mês Atual
          </button>
        </div>
      </div>
    </div>
  );

  // Skeleton loading
  if (isLoading || !clientId) {
    return (
      <div>
        <h2 className="text-lg font-semibold text-gray-800 mb-4">Produtos do Cliente</h2>
        <table className="min-w-full border border-gray-200">
          <thead>
            <tr>
              {["ID", "Produto", "Quantidade", "Valor Total"].map((col) => (
                <th
                  key={col}
                  className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700"
                >
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {[...Array(5)].map((_, idx) => (
              <tr key={idx} className="animate-pulse">
                <td className="px-4 py-2 border-b border-gray-100">
                  <div className="h-4 w-8 bg-gray-200 rounded opacity-60" />
                </td>
                <td className="px-4 py-2 border-b border-gray-100">
                  <div className="h-4 w-32 bg-gray-200 rounded opacity-60" />
                </td>
                <td className="px-4 py-2 border-b border-gray-100">
                  <div className="h-4 w-16 bg-gray-200 rounded opacity-60" />
                </td>
                <td className="px-4 py-2 border-b border-gray-100">
                  <div className="h-4 w-20 bg-gray-200 rounded opacity-60" />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
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

  // Monta os cabeçalhos dinamicamente
  const columns = Object.keys(products[0]);

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-800">Produtos do Cliente</h2>
      {renderDateFilter()}
      <table className="min-w-full border border-gray-200">
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col}
                className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700"
              >
                {col === "code"
                  ? "Código"
                  : col === "name"
                    ? "Produto"
                    : col === "price"
                      ? "Preço"
                      : col === "quantity"
                        ? "Quantidade"
                        : col === "totalValue"
                          ? "Valor Total"
                          : col}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {products.map((prod, idx) => (
            <tr key={prod.productId || idx} className="hover:bg-gray-50">
              {columns.map((col) => (
                <td
                  key={col}
                  className="px-4 py-2 border-b border-gray-100 text-sm text-gray-800 whitespace-nowrap"
                >
                  {(col === "totalValue" || col === "price")
                    ? `R$ ${Number(prod[col]).toLocaleString("pt-BR", {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}`
                    : prod[col]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}