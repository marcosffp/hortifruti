import { ProductInfo } from "@/types/productType";
import { getAuthHeaders } from "@/utils/httpUtils";
import { useEffect, useState } from "react";

interface ClientProductsTableProps {
  clientId: number | undefined;
  startDate?: string;
  endDate?: string;
}

export default function ClientProductsTable({ clientId, startDate, endDate }: ClientProductsTableProps) {
  const [products, setProducts] = useState<ProductInfo[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Datas padrão: mês atual
  const getDefaultDates = () => {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    return {
      start: firstDay.toISOString().slice(0, 10) + "T00:00:00",
      end: lastDay.toISOString().slice(0, 10) + "T23:59:59",
    };
  };

  useEffect(() => {
    const fetchProducts = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const { start, end } = getDefaultDates();
        const sDate = startDate || start;
        const eDate = endDate || end;

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

    if (clientId) fetchProducts();
  }, [clientId, startDate, endDate]);

  // Skeleton loading
  if (isLoading || !clientId) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-4 mt-6 overflow-x-auto">
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
      <div className="bg-red-100 border border-red-300 rounded-lg p-4 text-center mt-6">
        {error}
      </div>
    );
  }

  if (!products.length) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-4 text-center text-gray-500 mt-6">
        Nenhum produto encontrado para este cliente no período selecionado.
      </div>
    );
  }

  // Monta os cabeçalhos dinamicamente
  const columns = Object.keys(products[0]);

  return (
    <div className="bg-white rounded-lg shadow-sm p-4 mt-6 overflow-x-auto">
      <h2 className="text-lg font-semibold text-gray-800 mb-4">Produtos do Cliente</h2>
      <table className="min-w-full border border-gray-200">
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col}
                className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700"
              >
                {col === "productId"
                  ? "ID"
                  : col === "productName"
                  ? "Produto"
                  : col === "totalQuantity"
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
                  {col === "totalValue"
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