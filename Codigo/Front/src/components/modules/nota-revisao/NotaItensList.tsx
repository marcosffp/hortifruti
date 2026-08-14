import type { FiscalProductType } from "@/types/purchaseType";
import type { NumericField } from "@/utils/numericRow";
import NotaItemRow from "./NotaItemRow";
import type { RevisaoRow } from "./types";

interface NotaItensListProps {
  rows: RevisaoRow[];
  products: FiscalProductType[];
  loadingProducts: boolean;
  onChangeRowCode: (index: number, code: string) => void;
  onChangeRowField: (index: number, field: NumericField, value: number) => void;
  onRemoveRow: (index: number) => void;
}

export default function NotaItensList({
  rows,
  products,
  loadingProducts,
  onChangeRowCode,
  onChangeRowField,
  onRemoveRow,
}: NotaItensListProps) {
  return (
    <>
      <div className="hidden lg:flex gap-2 px-1 text-xs font-semibold text-gray-500 uppercase">
        <span className="flex-1">Produto (lido → selecione o do catálogo)</span>
        <span className="w-24 text-right">Qtd.</span>
        <span className="w-24 text-right">Preço Unit.</span>
        <span className="w-24 text-right">Total</span>
        <span className="w-9" />
      </div>

      <div className="space-y-3">
        {rows.map((row, index) => (
          <NotaItemRow
            // biome-ignore lint/suspicious/noArrayIndexKey: linhas vêm de um array fixo retornado pela extração, sem id próprio
            key={index}
            row={row}
            products={products}
            loadingProducts={loadingProducts}
            onChangeCode={(code) => onChangeRowCode(index, code)}
            onChangeField={(field, value) =>
              onChangeRowField(index, field, value)
            }
            onRemove={() => onRemoveRow(index)}
          />
        ))}
        {rows.length === 0 && (
          <p className="text-sm text-gray-500 italic">
            Nenhum item — todos foram removidos.
          </p>
        )}
      </div>
    </>
  );
}
