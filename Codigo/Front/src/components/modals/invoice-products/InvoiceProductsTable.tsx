"use client";

import type { InvoiceProductType } from "@/types/purchaseType";
import { formatCurrency } from "@/utils/formatCurrency";
import { calculateTotal } from "./formatters";
import InvoiceProductRow from "./InvoiceProductRow";

interface InvoiceProductsTableProps {
  products: InvoiceProductType[];
  editingProductId: number | null;
  editForm: Partial<InvoiceProductType>;
  loading: boolean;
  onEditChange: (
    field: keyof InvoiceProductType,
    value: string | number,
  ) => void;
  onStartEdit: (product: InvoiceProductType) => void;
  onCancelEdit: () => void;
  onSaveEdit: () => void;
  onDeleteClick: (productId: number) => void;
}

export default function InvoiceProductsTable({
  products,
  editingProductId,
  editForm,
  loading,
  onEditChange,
  onStartEdit,
  onCancelEdit,
  onSaveEdit,
  onDeleteClick,
}: InvoiceProductsTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr className="bg-gray-100 border-b border-gray-300">
            <th className="text-left p-3 font-semibold">Código</th>
            <th className="text-left p-3 font-semibold">Produto</th>
            <th className="text-right p-3 font-semibold">Quantidade</th>
            <th className="text-right p-3 font-semibold">Unidade</th>
            <th className="text-right p-3 font-semibold">Preço Unit.</th>
            <th className="text-right p-3 font-semibold">Total</th>
            <th className="text-center p-3 font-semibold">Ações</th>
          </tr>
        </thead>
        <tbody>
          {products.map((product) => (
            <InvoiceProductRow
              key={product.id}
              product={product}
              isEditing={editingProductId === product.id}
              editForm={editForm}
              loading={loading}
              onEditChange={onEditChange}
              onStartEdit={onStartEdit}
              onCancelEdit={onCancelEdit}
              onSaveEdit={onSaveEdit}
              onDeleteClick={onDeleteClick}
            />
          ))}
        </tbody>
        <tfoot>
          <tr className="bg-gray-50 font-semibold">
            <td colSpan={5} className="p-3 text-right">
              Total Geral:
            </td>
            <td className="p-3 text-right">
              {formatCurrency(
                products.reduce(
                  (sum, p) => sum + calculateTotal(p.price, p.quantity),
                  0,
                ),
              )}
            </td>
            <td></td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
