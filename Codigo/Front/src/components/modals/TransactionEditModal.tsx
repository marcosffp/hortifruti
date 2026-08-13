"use client";

import { X } from "lucide-react";
import type { FormEvent } from "react";
import type {
  TransactionRequest,
  TransactionResponse,
} from "@/services/transactionService";

interface TransactionEditModalProps {
  transaction: TransactionResponse;
  categories: string[];
  onClose: () => void;
  onSubmit: (data: TransactionRequest) => void;
}

export default function TransactionEditModal({
  transaction,
  categories,
  onClose,
  onSubmit,
}: TransactionEditModalProps) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-xl">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-semibold">Editar Lançamento</h2>
          <button
            type="button"
            className="text-gray-500 hover:text-gray-800"
            onClick={onClose}
          >
            <X size={24} />
          </button>
        </div>

        <form
          onSubmit={(e: FormEvent) => {
            e.preventDefault();
            const form = e.target as HTMLFormElement;
            const formData = new FormData(form);

            onSubmit({
              document: (formData.get("document") as string) || null,
              history: formData.get("history") as string,
              category: formData.get("category") as string,
              transactionType: formData.get("transactionType") as
                | "CREDITO"
                | "DEBITO",
              transactionDate: formData.get("transactionDate") as string,
              amount: parseFloat(formData.get("amount") as string),
              bank: formData.get("bank") as string,
              codHistory: formData.get("codHistory") as string,
              batch: formData.get("batch") as string,
              sourceAgency: formData.get("sourceAgency") as string,
            });
          }}
        >
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label
                htmlFor="edit-history"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Histórico
              </label>
              <input
                id="edit-history"
                type="text"
                name="history"
                defaultValue={transaction.history}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>

            <div>
              <label
                htmlFor="edit-category"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Categoria
              </label>
              <select
                id="edit-category"
                name="category"
                defaultValue={transaction.category}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              >
                {categories.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label
                htmlFor="edit-transactionType"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Tipo
              </label>
              <select
                id="edit-transactionType"
                name="transactionType"
                defaultValue={transaction.transactionType}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              >
                <option value="CREDITO">Entrada</option>
                <option value="DEBITO">Saída</option>
              </select>
            </div>

            <div>
              <label
                htmlFor="edit-transactionDate"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Data
              </label>
              <input
                id="edit-transactionDate"
                type="date"
                name="transactionDate"
                defaultValue={transaction.transactionDate}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>

            <div>
              <label
                htmlFor="edit-amount"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Valor
              </label>
              <input
                id="edit-amount"
                type="number"
                step="0.01"
                name="amount"
                defaultValue={transaction.amount}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>

            <div>
              <label
                htmlFor="edit-bank"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Banco
              </label>
              <input
                id="edit-bank"
                type="text"
                name="bank"
                defaultValue={transaction.bank}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>

            <div>
              <label
                htmlFor="edit-document"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Documento (opcional)
              </label>
              <input
                id="edit-document"
                type="text"
                name="document"
                defaultValue={transaction.document || ""}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>

            <div>
              <label
                htmlFor="edit-codHistory"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Código de Histórico*
              </label>
              <input
                id="edit-codHistory"
                type="text"
                name="codHistory"
                defaultValue={transaction.codHistory || "001"}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>

            <div>
              <label
                htmlFor="edit-batch"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Lote*
              </label>
              <input
                id="edit-batch"
                type="text"
                name="batch"
                defaultValue={transaction.batch || "001"}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>

            <div>
              <label
                htmlFor="edit-sourceAgency"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Agência de Origem*
              </label>
              <input
                id="edit-sourceAgency"
                type="text"
                name="sourceAgency"
                defaultValue={transaction.sourceAgency || "001"}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>
          </div>

          <div className="flex justify-end space-x-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-100"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
            >
              Salvar Alterações
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
