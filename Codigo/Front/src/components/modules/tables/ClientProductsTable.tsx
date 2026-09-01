import { useEffect, useRef, useState } from "react";
import SkeletonTableLoading from "@/components/ui/SkeletonTableLoading";
import { useGroupedProducts } from "@/hooks/useGroupedProducts";
import { usePurchase } from "@/hooks/usePurchase";
import type { GroupedProductRequest } from "@/types/groupedType";
import { getLastMonthInterval, getWeekInterval } from "@/utils/dateUtils";
import { showError, showSuccess } from "@/utils/toastUtils";

interface ClientProductsTableProps {
  clientId: number | undefined;
  refreshKey?: number;
}

export default function ClientProductsTable({
  clientId,
  refreshKey,
}: ClientProductsTableProps) {
  const [groupBy, setGroupBy] = useState<"week" | "month" | "custom">("custom");
  const [startDate, setStartDate] = useState(() => {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    return firstDay.toISOString().split("T")[0];
  });
  const [endDate, setEndDate] = useState(() => {
    const now = new Date();
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    return lastDay.toISOString().split("T")[0];
  });

  const [products, setProducts] = useState<GroupedProductRequest[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const debounceTimer = useRef<NodeJS.Timeout | null>(null);
  const { fetchClientProducts } = usePurchase();
  const { confirmGrouping } = useGroupedProducts();

  useEffect(() => {
    if (groupBy === "week") {
      const { start, end } = getWeekInterval();
      setStartDate(start);
      setEndDate(end);
    } else if (groupBy === "month") {
      const { start, end } = getLastMonthInterval();
      setStartDate(start);
      setEndDate(end);
    }
  }, [groupBy]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: refreshKey is intentionally unused inside the effect — it only exists to force a refetch when the parent bumps it
  useEffect(() => {
    if (!clientId) return;

    if (debounceTimer.current) clearTimeout(debounceTimer.current);

    debounceTimer.current = setTimeout(() => {
      const fetchProducts = async () => {
        setIsLoading(true);
        setError(null);
        try {
          const products = await fetchClientProducts(
            clientId,
            startDate,
            endDate,
          );
          setProducts(products);
        } catch (err) {
          setError(
            err instanceof Error
              ? err.message
              : "Erro ao buscar produtos do cliente",
          );
        } finally {
          setIsLoading(false);
        }
      };

      fetchProducts();
    }, 600);

    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [clientId, startDate, endDate, refreshKey, fetchClientProducts]);

  const handleConfirmGrouping = () => {
    if (!clientId) return;
    confirmGrouping(clientId, products)
      .then(() => {
        showSuccess("Agrupamento confirmado com sucesso!");
      })
      .catch((err) => {
        console.error(err);
        showError(
          err instanceof Error ? err.message : "Erro ao confirmar agrupamento.",
        );
      });
  };

  const columnLabels: Record<keyof GroupedProductRequest, string> = {
    code: "Código",
    name: "Produto",
    quantity: "Quantidade",
    price: "Preço",
    totalValue: "Valor Total",
  };

  const renderDateFilter = () => (
    <div className="mb-4 p-4 bg-white">
      <div className="flex flex-wrap *:flex-grow items-end gap-6">
        <div>
          <label
            htmlFor="client-products-groupby"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Tipo de Agrupamento
          </label>
          <select
            id="client-products-groupby"
            className="px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            value={groupBy}
            onChange={(e) =>
              setGroupBy(e.target.value as "week" | "month" | "custom")
            }
          >
            <option value="custom">Intervalo Personalizado</option>
            <option value="week">Semanal</option>
            <option value="month">Mensal</option>
          </select>
        </div>
        <div>
          <label
            htmlFor="client-products-data-inicial"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Data Inicial
          </label>
          <input
            id="client-products-data-inicial"
            type="date"
            value={startDate}
            disabled={groupBy !== "custom"}
            onChange={(e) => setStartDate(e.target.value)}
            className={`px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 ${groupBy !== "custom" ? "bg-gray-100 cursor-not-allowed" : ""}`}
          />
        </div>
        <div>
          <label
            htmlFor="client-products-data-final"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Data Final
          </label>
          <input
            id="client-products-data-final"
            type="date"
            value={endDate}
            disabled={groupBy !== "custom"}
            onChange={(e) => setEndDate(e.target.value)}
            className={`px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 ${groupBy !== "custom" ? "bg-gray-100 cursor-not-allowed" : ""}`}
          />
        </div>
        <div>
          <button
            type="button"
            onClick={handleConfirmGrouping}
            className={`px-4 w-full py-2 bg-[var(--primary-light)] text-white rounded-lg hover:bg-[var(--primary-dark)] cursor-pointer transition-colors ${products.length === 0 ? "opacity-50 cursor-not-allowed hover:bg-[var(--primary-light)]" : ""}`}
            disabled={products.length === 0}
          >
            Confirmar Agrupamento
          </button>
        </div>
      </div>
    </div>
  );

  if (isLoading || !clientId) {
    return (
      <SkeletonTableLoading
        title="Produtos do Cliente"
        cols={["Código", "Produto", "Preço", "Quantidade", "Valor Total"]}
      />
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
        <h2 className="text-lg font-semibold text-gray-800">
          Produtos do Cliente
        </h2>
        {renderDateFilter()}
        <p className="text-center text-gray-500">
          Nenhum produto encontrado para este cliente no período selecionado.
        </p>
      </div>
    );
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-800">
        Produtos do Cliente
      </h2>
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
                    {key === "totalValue" || key === "price"
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
