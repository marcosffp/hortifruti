export default function NotesTable() {
  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-800 mb-4">Notas Fiscais</h2>
      <table className="min-w-full border border-gray-200">
        <thead>
          <tr>
            <th className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700">Número</th>
            <th className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700">Data</th>
            <th className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700">Valor</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td className="px-4 py-2 border-b border-gray-100">12345</td>
            <td className="px-4 py-2 border-b border-gray-100">2025-10-01</td>
            <td className="px-4 py-2 border-b border-gray-100">R$ 1.234,56</td>
          </tr>
          <tr>
            <td className="px-4 py-2 border-b border-gray-100">67890</td>
            <td className="px-4 py-2 border-b border-gray-100">2025-10-05</td>
            <td className="px-4 py-2 border-b border-gray-100">R$ 987,65</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}