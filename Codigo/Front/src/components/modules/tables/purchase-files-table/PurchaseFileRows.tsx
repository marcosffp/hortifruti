import { Eye, Trash } from "lucide-react";
import type { PurchaseType } from "@/types/purchaseType";
import { formatCurrency } from "@/utils/formatCurrency";

interface PurchaseFileRowProps {
  purchase: PurchaseType;
  formatDate: (dateString: string) => string;
  onViewProducts: (purchase: PurchaseType) => void;
  onDelete: (purchaseId: number) => void;
}

/** Linha da tabela (desktop) de um arquivo de compra. */
export function PurchaseFileTableRow({
  purchase,
  formatDate,
  onViewProducts,
  onDelete,
}: PurchaseFileRowProps) {
  return (
    <tr className="border-b border-gray-300 hover:bg-gray-50 transition-colors">
      <td className="p-3">{formatDate(purchase.purchaseDate)}</td>
      <td className="p-3 font-semibold">{formatCurrency(purchase.total)}</td>
      <td className="p-3">{formatDate(purchase.updatedAt)}</td>
      <td className="p-3">
        <div className="flex items-center justify-center gap-2">
          <button
            type="button"
            onClick={() => onViewProducts(purchase)}
            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
            title="Ver produtos"
          >
            <Eye className="w-4 h-4" />
          </button>
          <button
            type="button"
            onClick={() => onDelete(purchase.id)}
            className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
            title="Deletar"
          >
            <Trash className="w-4 h-4" />
          </button>
        </div>
      </td>
    </tr>
  );
}

/** Card (mobile) de um arquivo de compra. */
export function PurchaseFileCard({
  purchase,
  formatDate,
  onViewProducts,
  onDelete,
}: PurchaseFileRowProps) {
  return (
    <div className="border border-gray-200 rounded-lg p-4">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-xs text-gray-500">Data da compra</p>
          <p className="font-medium text-gray-800">
            {formatDate(purchase.purchaseDate)}
          </p>
        </div>
        <p className="font-semibold text-gray-800">
          {formatCurrency(purchase.total)}
        </p>
      </div>
      <p className="text-xs text-gray-500 mt-2">
        Última atualização: {formatDate(purchase.updatedAt)}
      </p>
      <div className="flex items-center gap-2 mt-3 pt-3 border-t border-gray-100">
        <button
          type="button"
          onClick={() => onViewProducts(purchase)}
          className="flex-1 flex items-center justify-center gap-1.5 p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors text-sm font-medium"
        >
          <Eye className="w-4 h-4" />
          Ver produtos
        </button>
        <button
          type="button"
          onClick={() => onDelete(purchase.id)}
          className="flex-1 flex items-center justify-center gap-1.5 p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors text-sm font-medium"
        >
          <Trash className="w-4 h-4" />
          Deletar
        </button>
      </div>
    </div>
  );
}
