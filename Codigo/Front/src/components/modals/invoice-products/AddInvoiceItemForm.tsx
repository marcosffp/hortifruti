"use client";

import { Check, X } from "lucide-react";
import MaskedDecimalInput from "@/components/ui/MaskedDecimalInput";
import ProductAutocompleteField from "@/components/ui/ProductAutocompleteField";
import type { FiscalProductType } from "@/types/purchaseType";

export interface NewInvoiceItem {
  code: string;
  quantity: number;
  price: number;
}

interface AddInvoiceItemFormProps {
  catalogProducts: FiscalProductType[];
  loadingCatalog: boolean;
  newItem: NewInvoiceItem;
  onCodeChange: (code: string) => void;
  onQuantityChange: (quantity: number) => void;
  onPriceChange: (price: number) => void;
  saving: boolean;
  onSave: () => void;
  onCancel: () => void;
}

export default function AddInvoiceItemForm({
  catalogProducts,
  loadingCatalog,
  newItem,
  onCodeChange,
  onQuantityChange,
  onPriceChange,
  saving,
  onSave,
  onCancel,
}: AddInvoiceItemFormProps) {
  return (
    <div className="flex flex-col md:flex-row md:items-center gap-2 border border-gray-200 rounded-lg p-3 mb-4 bg-gray-50">
      <div className="flex-1">
        <ProductAutocompleteField
          products={catalogProducts}
          value={newItem.code}
          onSelect={onCodeChange}
          disabled={loadingCatalog || saving}
        />
      </div>
      <div className="flex gap-2">
        <MaskedDecimalInput
          value={newItem.quantity}
          onChange={onQuantityChange}
          placeholder="Qtd."
          disabled={saving}
          className="w-full md:w-24 p-2 border border-gray-300 rounded-lg text-right focus:outline-none focus:ring-1 focus:ring-green-500"
        />
        <MaskedDecimalInput
          value={newItem.price}
          onChange={onPriceChange}
          placeholder="Preço"
          disabled={saving}
          className="w-full md:w-24 p-2 border border-gray-300 rounded-lg text-right focus:outline-none focus:ring-1 focus:ring-green-500"
        />
        <button
          type="button"
          onClick={onSave}
          disabled={saving}
          className="p-2 text-green-600 hover:bg-green-100 rounded-lg transition-colors disabled:opacity-50"
          title="Salvar"
        >
          <Check className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={saving}
          className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50"
          title="Cancelar"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
