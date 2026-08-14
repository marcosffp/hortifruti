import { formatCurrency } from "@/utils/formatCurrency";
import { MARGEM_CONSISTENCIA } from "./helpers";

interface NotaTotaisResumoProps {
  totalLido: number;
  totalCalculado: number;
}

export default function NotaTotaisResumo({
  totalLido,
  totalCalculado,
}: NotaTotaisResumoProps) {
  const bateComOTotalLido =
    Math.abs(totalCalculado - totalLido) < MARGEM_CONSISTENCIA;

  return (
    <div className="pt-2 border-t border-gray-200 space-y-1">
      <div className="flex justify-between text-sm text-gray-500">
        <span>Total lido na nota</span>
        <span>{formatCurrency(totalLido)}</span>
      </div>
      <div className="flex justify-between font-semibold">
        <span>Total calculado (itens acima)</span>
        <span className={bateComOTotalLido ? "text-green-700" : "text-red-700"}>
          {formatCurrency(totalCalculado)}
        </span>
      </div>
      {!bateComOTotalLido && (
        <p className="text-xs text-red-600">
          ⚠ Não bate com o total lido na nota — confira os itens acima.
        </p>
      )}
    </div>
  );
}
