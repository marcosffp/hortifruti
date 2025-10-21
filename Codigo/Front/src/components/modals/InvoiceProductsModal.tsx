"use client";

import React, { useState, useEffect } from "react";
import { X, Trash2 } from "lucide-react";
import { toast } from "react-toastify";
import { purchaseService } from "@/services/purchaseService";
import { InvoiceProductType } from "@/types/purchaseType";
import ConfirmDeleteModal from "./ConfirmDeleteModal";

interface InvoiceProductsModalProps {
  purchaseId: number;
  onClose: () => void;
  onUpdate: () => void;
}

export default function InvoiceProductsModal({
  purchaseId,
  onClose,
  onUpdate,
}: InvoiceProductsModalProps) {
  const [products, setProducts] = useState<InvoiceProductType[]>([]);
  const [loading, setLoading] = useState(false);
  const [confirmDeleteModal, setConfirmDeleteModal] = useState({ state: false, productId: -1 });

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const data = await purchaseService.fetchInvoiceProducts(purchaseId);
      setProducts(data);
    } catch (error) {
      toast.error("Erro ao carregar produtos");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [purchaseId]);

  const handleDelete = async (productId: number) => {
    try {
      await purchaseService.deleteInvoiceProduct(productId);
      toast.success("Produto deletado com sucesso");
      fetchProducts();
      onUpdate();
    } catch (error) {
      toast.error("Erro ao deletar produto");
      console.error(error);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);
  };

  const calculateTotal = (price: number, quantity: number) => {
    return price * quantity;
  };

  return (
    <div className="fixed inset-0 h-full bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-5xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b border-gray-300">
          <h2 className="text-xl font-semibold">Produtos da Compra #{purchaseId}</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-16 bg-gray-200 animate-pulse rounded-lg" />
              ))}
            </div>
          ) : products.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              Nenhum produto encontrado
            </div>
          ) : (
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
                    <tr key={product.id} className="border-b border-gray-300 hover:bg-gray-50">
                      <td className="p-3">{product.code}</td>
                      <td className="p-3">{product.name}</td>
                      <td className="p-3 text-right">{product.quantity}</td>
                      <td className="p-3 text-right">{product.unitType}</td>
                      <td className="p-3 text-right">{formatCurrency(product.price)}</td>
                      <td className="p-3 text-right font-semibold">
                        {formatCurrency(calculateTotal(product.price, product.quantity))}
                      </td>
                      <td className="p-3 text-center">
                        <button
                          onClick={() => setConfirmDeleteModal({ state: true, productId: product.id })}
                          className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                          title="Deletar produto"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="bg-gray-50 font-semibold">
                    <td colSpan={5} className="p-3 text-right">
                      Total Geral:
                    </td>
                    <td className="p-3 text-right">
                      {formatCurrency(
                        products.reduce((sum, p) => sum + calculateTotal(p.price, p.quantity), 0)
                      )}
                    </td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 p-6 border-t border-gray-300">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
          >
            Fechar
          </button>
        </div>
      </div>

      <ConfirmDeleteModal
        open={confirmDeleteModal.state}
        onClose={() => setConfirmDeleteModal({ state: false, productId: -1 })}
        onConfirm={() => {
          handleDelete(confirmDeleteModal.productId);
          setConfirmDeleteModal({ state: false, productId: -1 });
        }}
      />
    </div>
  );
}
