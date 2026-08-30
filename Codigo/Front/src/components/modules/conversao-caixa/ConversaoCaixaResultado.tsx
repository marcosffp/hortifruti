import type { ConversaoCaixaImportResponse } from "@/types/conversaoCaixaType";

function formatKg(valor: number): string {
  return valor.toLocaleString("pt-BR", {
    minimumFractionDigits: 3,
    maximumFractionDigits: 3,
  });
}

export default function ConversaoCaixaResultado({
  resultado,
}: {
  resultado: ConversaoCaixaImportResponse;
}) {
  const {
    cadastrados,
    atualizados,
    semAlteracao,
    codigosNaoEncontrados,
    conflitosNoArquivo,
  } = resultado;

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5 space-y-6">
      <div className="flex flex-wrap gap-2">
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-800">
          {cadastrados.length} cadastrado(s)
        </span>
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800">
          {atualizados.length} atualizado(s)
        </span>
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-700">
          {semAlteracao.length} sem alteração
        </span>
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-800">
          {codigosNaoEncontrados.length} não encontrado(s)
        </span>
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-yellow-100 text-yellow-800">
          {conflitosNoArquivo.length} conflito(s)
        </span>
      </div>

      {conflitosNoArquivo.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-800 mb-2">
            ⚠ Conflitos no arquivo — mesmo código com pesos diferentes
          </h2>
          <div className="space-y-2">
            {conflitosNoArquivo.map((conflito) => (
              <div
                key={conflito.codigo}
                className="border border-yellow-200 bg-yellow-50 rounded-lg p-3 text-sm text-gray-700"
              >
                Código{" "}
                <span className="font-mono font-medium">{conflito.codigo}</span>{" "}
                apareceu com pesos{" "}
                {conflito.valoresEncontrados.map(formatKg).join(" kg, ")} kg no
                mesmo arquivo — aplicado o primeiro valor encontrado:{" "}
                <span className="font-semibold">
                  {formatKg(conflito.valorAplicado)} kg
                </span>
                . Corrija a planilha se não for o valor certo.
              </div>
            ))}
          </div>
        </section>
      )}

      {cadastrados.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-800 mb-2">
            Cadastrados
          </h2>
          <div className="overflow-x-auto border border-gray-100 rounded-lg">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-4 py-2 font-semibold text-gray-700">
                    Código
                  </th>
                  <th className="text-left px-4 py-2 font-semibold text-gray-700">
                    Produto
                  </th>
                  <th className="text-right px-4 py-2 font-semibold text-gray-700">
                    Peso da caixa
                  </th>
                </tr>
              </thead>
              <tbody>
                {cadastrados.map((item) => (
                  <tr key={item.codigo} className="border-b border-gray-50">
                    <td className="px-4 py-2 font-mono">{item.codigo}</td>
                    <td className="px-4 py-2 text-gray-700">{item.produto}</td>
                    <td className="px-4 py-2 text-right text-gray-700">
                      {formatKg(item.pesoCaixaKg)} kg
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {atualizados.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-800 mb-2">
            Atualizados
          </h2>
          <div className="overflow-x-auto border border-gray-100 rounded-lg">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-4 py-2 font-semibold text-gray-700">
                    Código
                  </th>
                  <th className="text-left px-4 py-2 font-semibold text-gray-700">
                    Produto
                  </th>
                  <th className="text-right px-4 py-2 font-semibold text-gray-700">
                    Peso anterior
                  </th>
                  <th className="text-right px-4 py-2 font-semibold text-gray-700">
                    Peso novo
                  </th>
                </tr>
              </thead>
              <tbody>
                {atualizados.map((item) => (
                  <tr key={item.codigo} className="border-b border-gray-50">
                    <td className="px-4 py-2 font-mono">{item.codigo}</td>
                    <td className="px-4 py-2 text-gray-700">{item.produto}</td>
                    <td className="px-4 py-2 text-right text-gray-500">
                      {formatKg(item.pesoAnterior)} kg
                    </td>
                    <td className="px-4 py-2 text-right text-gray-700 font-medium">
                      {formatKg(item.pesoNovo)} kg
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {codigosNaoEncontrados.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-800 mb-2">
            Códigos não encontrados no catálogo
          </h2>
          <p className="text-sm text-gray-600">
            Produto ainda não cadastrado no catálogo fiscal — revise
            manualmente:{" "}
            <span className="font-mono">
              {codigosNaoEncontrados.join(", ")}
            </span>
          </p>
        </section>
      )}

      {semAlteracao.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-800 mb-2">
            Sem alteração
          </h2>
          <p className="text-sm text-gray-500">
            Peso já cadastrado igual ao do arquivo:{" "}
            <span className="font-mono">{semAlteracao.join(", ")}</span>
          </p>
        </section>
      )}
    </div>
  );
}
