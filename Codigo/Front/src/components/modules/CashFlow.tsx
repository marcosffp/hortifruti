import Card from "../ui/Card";

export default function CashFlow() {
  const data = [
    {
      date: "14/01/2024",
      entradas: 4474.75,
      saidas: 1760.8,
      saldo: 2713.95,
      acumulado: 15420.3,
    },
    {
      date: "13/01/2024",
      entradas: 3250.8,
      saidas: 2100.45,
      saldo: 1150.35,
      acumulado: 12706.35,
    },
  ];

  return (
    <div className="grid gap-6">
      <Card title="Resumo Financeiro">
        <div className="grid grid-cols-4 gap-4">
          <div className="bg-green-50 p-4 rounded-lg">
            Saldo Atual: R$ 15.420,30
          </div>
          <div className="bg-green-50 p-4 rounded-lg">
            Entradas: R$ 19.417,20
          </div>
          <div className="bg-red-50 p-4 rounded-lg">Saídas: R$ 9.972,90</div>
          <div className="bg-green-50 p-4 rounded-lg">
            Saldo Período: R$ 9.444,30
          </div>
        </div>
      </Card>

      <Card title="Movimentação Diária">
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr className="text-left border-b">
              <th className="p-2">Data</th>
              <th className="p-2">Entradas</th>
              <th className="p-2">Saídas</th>
              <th className="p-2">Saldo</th>
              <th className="p-2">Acumulado</th>
            </tr>
          </thead>
          <tbody>
            {data.map((row) => (
              <tr
                key={`cashflow-${row.date}-${row.acumulado}`}
                className="border-b"
              >
                <td className="p-2">{row.date}</td>
                <td className="p-2 text-green-600">
                  +R$ {row.entradas.toFixed(2)}
                </td>
                <td className="p-2 text-red-600">
                  -R$ {row.saidas.toFixed(2)}
                </td>
                <td className="p-2 text-green-600">
                  +R$ {row.saldo.toFixed(2)}
                </td>
                <td className="p-2">R$ {row.acumulado.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
