import { useEffect, useState } from "react";

interface GroupedProductsTableProps {
  clientId: number | undefined;
  refreshKey?: number;
}

export default function GroupedProductsTable({ clientId, refreshKey }: GroupedProductsTableProps) {
  const [grouped, setGrouped] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!clientId) return;
    setIsLoading(true);
    setError(null);
    fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/purchases/client/products?clientId=${clientId}&startDate=2025-10-01T00:00:00&endDate=2025-10-31T23:59:59`
    )
      .then((res) => res.json())
      .then((data) => setGrouped(data.products || []))
      .catch(() => setError("Erro ao buscar produtos agrupados"))
      .finally(() => setIsLoading(false));
  }, [clientId, refreshKey]);

  if (!clientId) return null;
  if (isLoading) return <div className="p-4">Carregando...</div>;
  if (error) return <div className="p-4 text-red-600">{error}</div>;
  if (!grouped.length) return <div className="p-4 text-gray-500">Nenhum produto agrupado encontrado.</div>;

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-800 mb-4">Produtos Agrupados</h2>
      <table className="min-w-full border border-gray-200">
        <thead>
          <tr>
            {Object.keys(grouped[0]).map((col) => (
              <th key={col} className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700">
                {col}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {grouped.map((row, idx) => (
            <tr key={idx}>
              {Object.keys(row).map((col) => (
                <td key={col} className="px-4 py-2 border-b border-gray-100">{row[col]}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}