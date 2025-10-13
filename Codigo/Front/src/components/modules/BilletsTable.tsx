export default function BilletsTable() {
  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-800 mb-4">Boletos</h2>
      <table className="min-w-full border border-gray-200">
        <thead>
          <tr>
            <th className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700">Número</th>
            <th className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700">Vencimento</th>
            <th className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700">Valor</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td className="px-4 py-2 border-b border-gray-100">BOL123</td>
            <td className="px-4 py-2 border-b border-gray-100">2025-10-10</td>
            <td className="px-4 py-2 border-b border-gray-100">R$ 500,00</td>
          </tr>
          <tr>
            <td className="px-4 py-2 border-b border-gray-100">BOL456</td>
            <td className="px-4 py-2 border-b border-gray-100">2025-10-15</td>
            <td className="px-4 py-2 border-b border-gray-100">R$ 750,00</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}