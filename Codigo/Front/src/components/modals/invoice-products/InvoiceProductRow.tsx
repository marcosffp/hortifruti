"use client";

import { Check, Edit, Trash2, X } from "lucide-react";
import type { InvoiceProductType } from "@/types/purchaseType";
import { formatCurrency } from "@/utils/formatCurrency";
import { calculateTotal, formatQuantity } from "./formatters";

interface InvoiceProductRowProps {
  product: InvoiceProductType;
  isEditing: boolean;
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

export default function InvoiceProductRow({
  product,
  isEditing,
  editForm,
  loading,
  onEditChange,
  onStartEdit,
  onCancelEdit,
  onSaveEdit,
  onDeleteClick,
}: InvoiceProductRowProps) {
  return (
    <tr className="border-b border-gray-300 hover:bg-gray-50">
      <td className="p-3">
        {isEditing ? (
          <input
            value={editForm.code ?? ""}
            onChange={(e) => onEditChange("code", e.target.value)}
            className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            disabled={loading}
          />
        ) : (
          product.code
        )}
      </td>
      <td className="p-3">
        {isEditing ? (
          <input
            value={editForm.name ?? ""}
            onChange={(e) => onEditChange("name", e.target.value)}
            className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            disabled={loading}
          />
        ) : (
          product.name
        )}
      </td>
      <td className="p-3 text-right">
        {isEditing ? (
          <input
            type="number"
            step="0.001"
            value={editForm.quantity ?? ""}
            onChange={(e) => onEditChange("quantity", e.target.value)}
            className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            disabled={loading}
          />
        ) : (
          formatQuantity(product.quantity)
        )}
      </td>
      <td className="p-3 text-right">
        {isEditing ? (
          <input
            value={editForm.unitType ?? ""}
            onChange={(e) => onEditChange("unitType", e.target.value)}
            className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            disabled={loading}
          />
        ) : (
          product.unitType
        )}
      </td>
      <td className="p-3 text-right">
        {isEditing ? (
          <input
            type="number"
            step="0.01"
            value={editForm.price ?? ""}
            onChange={(e) => onEditChange("price", e.target.value)}
            className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            disabled={loading}
          />
        ) : (
          formatCurrency(product.price)
        )}
      </td>
      <td className="p-3 text-right font-semibold">
        {formatCurrency(calculateTotal(product.price, product.quantity))}
      </td>
      <td className="p-3 flex items-center justify-center gap-2">
        {isEditing ? (
          <div className="flex items-center justify-cente">
            <button
              type="button"
              onClick={onSaveEdit}
              className="p-2 text-green-600 hover:bg-green-50 rounded-lg transition-colors"
              title="Salvar"
              disabled={loading}
            >
              <Check className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={onCancelEdit}
              className="p-2 text-gray-600 hover:bg-gray-50 rounded-lg transition-colors"
              title="Cancelar"
              disabled={loading}
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => onStartEdit(product)}
            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
            title="Editar"
          >
            <Edit className="w-4 h-4" />
          </button>
        )}
        <button
          type="button"
          onClick={() => onDeleteClick(product.id)}
          className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          title="Deletar produto"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </td>
    </tr>
  );
}
