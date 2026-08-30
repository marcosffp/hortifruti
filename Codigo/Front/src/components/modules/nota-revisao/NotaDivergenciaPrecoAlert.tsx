interface NotaDivergenciaPrecoAlertProps {
  itensComDivergenciaPreco: string[];
  semTabelaPrecoParaCompetencia: boolean | null;
}

export default function NotaDivergenciaPrecoAlert({
  itensComDivergenciaPreco,
  semTabelaPrecoParaCompetencia,
}: NotaDivergenciaPrecoAlertProps) {
  if (semTabelaPrecoParaCompetencia) {
    return (
      <div className="bg-gray-100 border border-gray-300 rounded-lg p-3 text-sm text-gray-700">
        <p className="font-semibold">
          ℹ Não existe tabela de preços confirmada pra esse cliente cobrindo a
          data desta nota.
        </p>
        <p className="mt-1">
          Os preços lidos na nota foram mantidos como estão — não foi possível
          conferir contra a tabela oficial.
        </p>
      </div>
    );
  }

  if (itensComDivergenciaPreco.length === 0) return null;

  return (
    <div className="bg-orange-50 border border-orange-300 rounded-lg p-3 text-sm text-orange-800">
      <p className="font-semibold">
        ⚠ Preço lido diverge da tabela de preços confirmada do cliente.
      </p>
      <p className="mt-1">
        Itens afetados:{" "}
        <span className="font-medium">
          {itensComDivergenciaPreco.join(", ")}
        </span>{" "}
        — o campo de preço abaixo já foi ajustado pro valor da tabela oficial
        (marcado com{" "}
        <span className="font-medium">lido: R$ X,XX — tabela aplicada</span>).
      </p>
    </div>
  );
}
