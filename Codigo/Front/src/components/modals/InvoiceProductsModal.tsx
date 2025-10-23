"use client";

import React, { useState, useEffect } from "react";
import { X, Trash2, Edit, Check } from "lucide-react";
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

  // ADICIONADOS: estados para edição inline
  const [editingProductId, setEditingProductId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<Partial<InvoiceProductType>>({});

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

  // Funções de edição
  const startEdit = (product: InvoiceProductType) => {
    setEditingProductId(product.id);
    setEditForm({
      code: product.code,
      name: product.name,
      price: product.price,
      quantity: product.quantity,
      unitType: product.unitType,
    });
  };

  const cancelEdit = () => {
    setEditingProductId(null);
    setEditForm({});
  };

  const handleEditChange = (field: keyof InvoiceProductType, value: string | number) => {
    setEditForm((prev) => ({ ...prev, [field]: value }));
  };

  const saveEdit = async () => {
    if (editingProductId == null) return;
    try {
      setLoading(true);
      const payload = {
        code: editForm.code,
        name: editForm.name,
        price: typeof editForm.price === "string" ? parseFloat(editForm.price) : editForm.price,
        quantity:
          typeof editForm.quantity === "string" ? parseInt(editForm.quantity as string, 10) : editForm.quantity,
        unitType: editForm.unitType,
      };
      const updated = await purchaseService.updateInvoiceProduct(editingProductId, payload);
      // atualizar lista local sem refetch completo
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      toast.success("Produto atualizado com sucesso");
      setEditingProductId(null);
      setEditForm({});
      onUpdate();
    } catch (error) {
      console.error(error);
      toast.error("Erro ao atualizar produto");
    } finally {
      setLoading(false);
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
                      <td className="p-3">
                        {editingProductId === product.id ? (
                          <input
                            value={editForm.code ?? ""}
                            onChange={(e) => handleEditChange("code" as any, e.target.value)}
                            className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
                            disabled={loading}
                          />
                        ) : (
                          product.code
                        )}
                      </td>
                      <td className="p-3">
                        {editingProductId === product.id ? (
                          <input
                            value={editForm.name ?? ""}
                            onChange={(e) => handleEditChange("name" as any, e.target.value)}
                            className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
                            disabled={loading}
                          />
                        ) : (
                          product.name
                        )}
                      </td>
                      <td className="p-3 text-right">
                        {editingProductId === product.id ? (
                          <input
                            type="number"
                            value={editForm.quantity ?? ""}
                            onChange={(e) => handleEditChange("quantity" as any, e.target.value)}
                            className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
                            disabled={loading}
                          />
                        ) : (
                          product.quantity
                        )}
                      </td>
                      <td className="p-3 text-right">
                        {editingProductId === product.id ? (
                          <input
                            value={editForm.unitType ?? ""}
                            onChange={(e) => handleEditChange("unitType" as any, e.target.value)}
                            className="w-full p-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
                            disabled={loading}
                          />
                        ) : (
                          product.unitType
                        )}
                      </td>
                      <td className="p-3 text-right">
                        {editingProductId === product.id ? (
                          <input
                            type="text"
                            value={formatCurrency(editForm.price ?? 0)}
                            onChange={(e) => handleEditChange("price" as any, e.target.value)}
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
                        {editingProductId === product.id ? (
                          <div className="flex items-center justify-cente">
                            <button
                              onClick={saveEdit}
                              className="p-2 text-green-600 hover:bg-green-50 rounded-lg transition-colors"
                              title="Salvar"
                              disabled={loading}
                            >
                              <Check className="w-4 h-4" />
                            </button>
                            <button
                              onClick={cancelEdit}
                              className="p-2 text-gray-600 hover:bg-gray-50 rounded-lg transition-colors"
                              title="Cancelar"
                              disabled={loading}
                            >
                              <X className="w-4 h-4" />
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => startEdit(product)}
                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="Editar"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                        )}
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
