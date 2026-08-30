"use client";

import { Plus, Trash2, X } from "lucide-react";
import { useEffect, useState } from "react";
import MaskedDecimalInput from "@/components/ui/MaskedDecimalInput";
import ProductAutocompleteField from "@/components/ui/ProductAutocompleteField";
import { useCapturaNota } from "@/hooks/useCapturaNota";
import { useFiscalProduct } from "@/hooks/useFiscalProduct";
import { usePurchase } from "@/hooks/usePurchase";
import type { FiscalProductType } from "@/types/purchaseType";
import { todaySaoPaulo } from "@/utils/dateUtils";
import { formatCurrency } from "@/utils/formatCurrency";
import {
  type NumericField,
  type NumericRow,
  recalcNumericRow,
  round,
} from "@/utils/numericRow";
import { showError, showSuccess } from "@/utils/toastUtils";

interface CreateManualPurchaseModalProps {
  clientId: number;
  onClose: () => void;
  onCreated: () => void;
}

interface ManualItemRow extends NumericRow {
  code: string;
}

function emptyRow(): ManualItemRow {
  return { code: "", quantity: 0, price: 0, total: 0, lastEdited: [] };
}

export default function CreateManualPurchaseModal({
  clientId,
  onClose,
  onCreated,
}: CreateManualPurchaseModalProps) {
  const [products, setProducts] = useState<FiscalProductType[]>([]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [purchaseDate, setPurchaseDate] = useState(() => todaySaoPaulo());
  const [rows, setRows] = useState<ManualItemRow[]>([emptyRow()]);
  const [submitting, setSubmitting] = useState(false);
  const [precosVigentes, setPrecosVigentes] = useState<Record<string, number>>(
    {},
  );
  const { getFiscalProducts } = useFiscalProduct();
  const { createManualPurchase } = usePurchase();
  const { buscarPrecosVigentes } = useCapturaNota();

  useEffect(() => {
    getFiscalProducts()
      .then(setProducts)
      .catch((error) => {
        showError("Erro ao carregar catálogo de produtos");
        console.error(error);
      })
      .finally(() => setLoadingProducts(false));
  }, [getFiscalProducts]);

  useEffect(() => {
    let cancelado = false;
    buscarPrecosVigentes(clientId, purchaseDate)
      .then((precos) => {
        if (cancelado) return;
        setPrecosVigentes(precos);
        setRows((prev) =>
          prev.map((row) => {
            const precoTabela = row.code ? precos[row.code] : undefined;
            if (precoTabela == null || precoTabela === row.price) return row;
            return {
              ...row,
              price: precoTabela,
              total: round(row.quantity * precoTabela, 2),
            };
          }),
        );
      })
      .catch((error) => console.error(error));
    return () => {
      cancelado = true;
    };
  }, [clientId, purchaseDate, buscarPrecosVigentes]);

  const updateRowCode = (index: number, code: string) => {
    setRows((prev) =>
      prev.map((row, i) => {
        if (i !== index) return row;
        const precoTabela = precosVigentes[code];
        if (precoTabela == null) return { ...row, code };
        return {
          ...row,
          code,
          price: precoTabela,
          total: round(row.quantity * precoTabela, 2),
        };
      }),
    );
  };

  const updateRowField = (
    index: number,
    field: NumericField,
    value: number,
  ) => {
    setRows((prev) =>
      prev.map((row, i) =>
        i === index ? recalcNumericRow(row, field, value) : row,
      ),
    );
  };

  const addRow = () => setRows((prev) => [...prev, emptyRow()]);

  const removeRow = (index: number) => {
    setRows((prev) => prev.filter((_, i) => i !== index));
  };

  const grandTotal = rows.reduce((sum, row) => sum + row.total, 0);

  const isValid =
    rows.length > 0 &&
    rows.every((row) => row.code !== "" && row.quantity > 0 && row.price >= 0);

  const handleSubmit = async () => {
    if (!isValid) {
      showError(
        "Preencha produto (selecionado da lista), quantidade e preço em todos os itens.",
      );
      return;
    }

    setSubmitting(true);
    try {
      await createManualPurchase({
        clientId,
        purchaseDate,
        items: rows.map((row) => ({
          code: row.code,
          quantity: row.quantity,
          price: row.price,
        })),
      });
      showSuccess("Compra criada com sucesso!");
      onCreated();
      onClose();
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Erro ao criar compra",
      );
      console.error(error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 h-full bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        <div className="flex justify-between items-center p-6 border-b border-gray-300">
          <h2 className="text-xl font-semibold">Criar Compra Manualmente</h2>
          <button
            type="button"
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          <div>
            <label
              htmlFor="manual-purchase-date"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              Data da Compra
            </label>
            <input
              id="manual-purchase-date"
              type="date"
              value={purchaseDate}
              onChange={(e) => setPurchaseDate(e.target.value)}
              className="w-full max-w-xs px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>

          <div className="hidden md:flex gap-2 px-1 text-xs font-semibold text-gray-500 uppercase">
            <span className="flex-1">Produto</span>
            <span className="w-24 text-right">Quantidade</span>
            <span className="w-24 text-right">Preço Unit.</span>
            <span className="w-24 text-right">Total</span>
            <span className="w-9" />
          </div>

          <div className="space-y-3">
            {rows.map((row, index) => (
              <div
                // biome-ignore lint/suspicious/noArrayIndexKey: linhas não têm identidade estável antes de serem criadas, a ordem é o único sinal disponível
                key={index}
                className="flex flex-col md:flex-row md:items-center gap-2 border border-gray-200 md:border-0 rounded-lg p-3 md:p-0"
              >
                <div className="flex-1">
                  <ProductAutocompleteField
                    products={products}
                    value={row.code}
                    onSelect={(code) => updateRowCode(index, code)}
                    disabled={loadingProducts}
                  />
                </div>
                <div className="flex gap-2">
                  <MaskedDecimalInput
                    value={row.quantity}
                    onChange={(quantity) =>
                      updateRowField(index, "quantity", quantity)
                    }
                    decimalPlaces={3}
                    placeholder="0,000"
                    className="w-full md:w-24 p-2 border border-gray-300 rounded-lg text-right focus:outline-none focus:ring-1 focus:ring-green-500"
                  />
                  <MaskedDecimalInput
                    value={row.price}
                    onChange={(price) => updateRowField(index, "price", price)}
                    placeholder="0,00"
                    className="w-full md:w-24 p-2 border border-gray-300 rounded-lg text-right focus:outline-none focus:ring-1 focus:ring-green-500"
                  />
                  <MaskedDecimalInput
                    value={row.total}
                    onChange={(total) => updateRowField(index, "total", total)}
                    placeholder="0,00"
                    className="w-full md:w-24 p-2 border border-gray-300 rounded-lg text-right font-semibold focus:outline-none focus:ring-1 focus:ring-green-500"
                  />
                  <button
                    type="button"
                    onClick={() => removeRow(index)}
                    disabled={rows.length === 1}
                    className="w-9 flex items-center justify-center text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                    title="Remover item"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="flex justify-between items-center pt-2 border-t border-gray-200">
            <button
              type="button"
              onClick={addRow}
              className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-green-700 hover:bg-green-50 rounded-lg transition-colors"
            >
              <Plus className="w-4 h-4" />
              Adicionar item
            </button>
            <p className="font-semibold pr-2">
              Total Geral: {formatCurrency(grandTotal)}
            </p>
          </div>
        </div>

        <div className="flex justify-end gap-3 p-6 border-t border-gray-300">
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={submitting || !isValid}
            className={`px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors ${
              submitting || !isValid ? "opacity-50 cursor-not-allowed" : ""
            }`}
          >
            {submitting ? "Criando..." : "Criar Compra"}
          </button>
        </div>
      </div>
    </div>
  );
}
