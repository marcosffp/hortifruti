import type { TabelaPrecoImportResponse } from "@/types/tabelaPrecoClienteType";

export default function TabelaPrecoImportResultado({
  resultado,
}: {
  resultado: TabelaPrecoImportResponse;
}) {
  const {
    autoAplicadosPorMapeamento,
    sugeridosAltaConfianca,
    sugeridosBaixaConfianca,
    semCorrespondencia,
    precosEmBrancoNoArquivo,
  } = resultado;

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5 space-y-4">
      <div className="flex flex-wrap gap-2">
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-800">
          {autoAplicadosPorMapeamento.length} aplicado(s) automaticamente
        </span>
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800">
          {sugeridosAltaConfianca.length} sugestão(ões) de alta confiança
        </span>
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-yellow-100 text-yellow-800">
          {sugeridosBaixaConfianca.length} sugestão(ões) pra revisar com atenção
        </span>
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-800">
          {semCorrespondencia.length} sem correspondência
        </span>
        {precosEmBrancoNoArquivo > 0 && (
          <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-700">
            {precosEmBrancoNoArquivo} item(ns) sem preço cotado esse mês
          </span>
        )}
      </div>
      <p className="text-sm text-gray-600">
        Nenhum vínculo vira preço oficial sozinho — revise cada item sugerido
        abaixo (ou confirme em lote os de alta confiança que já batem com um
        vínculo anterior) antes de fechar a tabela.
      </p>
    </div>
  );
}
