import { Trash2 } from "lucide-react";
import MaskedDecimalInput from "@/components/ui/MaskedDecimalInput";
import ProductAutocompleteField from "@/components/ui/ProductAutocompleteField";
import type { FiscalProductType } from "@/types/purchaseType";
import type { NumericField } from "@/utils/numericRow";
import { itemBate } from "./helpers";
import { CONFIANCA_BADGE, type RevisaoRow } from "./types";

interface NotaItemRowProps {
  row: RevisaoRow;
  products: FiscalProductType[];
  loadingProducts: boolean;
  onChangeCode: (code: string) => void;
  onChangeField: (field: NumericField, value: number) => void;
  onRemove: () => void;
}

export default function NotaItemRow({
  row,
  products,
  loadingProducts,
  onChangeCode,
  onChangeField,
  onRemove,
}: NotaItemRowProps) {
  return (
    <div className="border border-gray-200 rounded-lg p-3 space-y-2">
      <p className="text-xs text-gray-500 flex items-center gap-2">
        <span>
          Lido:{" "}
          <span className="font-medium text-gray-700">{row.produtoLido}</span>
          {row.unidadeLida ? ` (${row.unidadeLida})` : ""}
        </span>
        {row.confianca && (
          <span
            className={`px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase ${CONFIANCA_BADGE[row.confianca]}`}
          >
            {row.confianca}
          </span>
        )}
        {row.produtoSugerido && (
          <span className="text-gray-400">
            sugerido: {row.produtoSugerido.nome} (
            {Math.round(row.produtoSugerido.score * 100)}%)
          </span>
        )}
        {!itemBate(row) && (
          <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase bg-red-100 text-red-800">
            ⚠ qtd × preço ≠ total
          </span>
        )}
        {row.precoLidoOriginal != null && (
          <span
            className={`px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase ${
              row.divergenciaPreco
                ? "bg-orange-100 text-orange-800"
                : "bg-gray-100 text-gray-600"
            }`}
            title="Preço aplicado vem da tabela de preços confirmada do cliente, não do que foi lido na nota"
          >
            lido: R${" "}
            {row.precoLidoOriginal.toLocaleString("pt-BR", {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}{" "}
            — tabela aplicada
          </span>
        )}
        {row.conversaoEstimada && row.quantidadeKgConvertida != null && (
          <span
            className="px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase bg-blue-100 text-blue-800"
            title="Peso calculado a partir do peso médio de caixa cadastrado para o produto, não do peso real dessa caixa"
          >
            convertido de caixa: ~
            {row.quantidadeKgConvertida.toLocaleString("pt-BR", {
              minimumFractionDigits: 3,
              maximumFractionDigits: 3,
            })}{" "}
            kg
            {row.precoPorKgConvertido != null &&
              ` (R$ ${row.precoPorKgConvertido.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}/kg)`}
          </span>
        )}
      </p>
      <div className="flex flex-col lg:flex-row lg:items-center gap-2">
        <div className="flex-1">
          <ProductAutocompleteField
            products={products}
            value={row.code}
            onSelect={onChangeCode}
            disabled={loadingProducts}
          />
        </div>
        <div className="flex gap-2">
          <MaskedDecimalInput
            value={row.quantity}
            onChange={(quantity) => onChangeField("quantity", quantity)}
            decimalPlaces={3}
            placeholder="0,000"
            className="w-full lg:w-24 p-2 border border-gray-300 rounded-lg text-right focus:outline-none focus:ring-1 focus:ring-green-500"
          />
          <MaskedDecimalInput
            value={row.price}
            onChange={(price) => onChangeField("price", price)}
            placeholder="0,00"
            className="w-full lg:w-24 p-2 border border-gray-300 rounded-lg text-right focus:outline-none focus:ring-1 focus:ring-green-500"
          />
          <MaskedDecimalInput
            value={row.total}
            onChange={(total) => onChangeField("total", total)}
            placeholder="0,00"
            className="w-full lg:w-24 p-2 border border-gray-300 rounded-lg text-right font-semibold focus:outline-none focus:ring-1 focus:ring-green-500"
          />
          <button
            type="button"
            onClick={onRemove}
            className="w-9 flex items-center justify-center text-red-600 hover:bg-red-50 rounded-lg transition-colors"
            title="Remover item"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
