"use client";

import { Plus, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { toast } from "react-toastify";
import { useFiscalProduct } from "@/hooks/useFiscalProduct";
import { usePurchase } from "@/hooks/usePurchase";
import type {
  FiscalProductType,
  InvoiceProductType,
} from "@/types/purchaseType";
import ConfirmDeleteModal from "./ConfirmDeleteModal";
import AddInvoiceItemForm from "./invoice-products/AddInvoiceItemForm";
import InvoiceProductsTable from "./invoice-products/InvoiceProductsTable";

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
  const [confirmDeleteModal, setConfirmDeleteModal] = useState({
    state: false,
    productId: -1,
  });

  const [editingProductId, setEditingProductId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<Partial<InvoiceProductType>>({});

  const [catalogProducts, setCatalogProducts] = useState<FiscalProductType[]>(
    [],
  );
  const [loadingCatalog, setLoadingCatalog] = useState(true);
  const [addingItem, setAddingItem] = useState(false);
  const [newItem, setNewItem] = useState({ code: "", quantity: 0, price: 0 });
  const [savingNewItem, setSavingNewItem] = useState(false);
  const { getFiscalProducts } = useFiscalProduct();
  const {
    fetchInvoiceProducts,
    addInvoiceProduct,
    updateInvoiceProduct,
    deleteInvoiceProduct,
  } = usePurchase();

  useEffect(() => {
    getFiscalProducts()
      .then(setCatalogProducts)
      .catch((error) => {
        toast.error("Erro ao carregar catálogo de produtos");
        console.error(error);
      })
      .finally(() => setLoadingCatalog(false));
  }, [getFiscalProducts]);

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchInvoiceProducts(purchaseId);
      setProducts(data);
    } catch (error) {
      toast.error("Erro ao carregar produtos");
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [purchaseId, fetchInvoiceProducts]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const handleDelete = async (productId: number) => {
    try {
      await deleteInvoiceProduct(productId);
      toast.success("Produto deletado com sucesso");
      fetchProducts();
      onUpdate();
    } catch (error) {
      toast.error("Erro ao deletar produto");
      console.error(error);
    }
  };

  const handleAddItem = async () => {
    if (!newItem.code || newItem.quantity <= 0 || newItem.price < 0) {
      toast.error("Selecione um produto e informe quantidade e preço válidos");
      return;
    }
    setSavingNewItem(true);
    try {
      await addInvoiceProduct(purchaseId, newItem);
      toast.success("Produto adicionado com sucesso");
      setAddingItem(false);
      setNewItem({ code: "", quantity: 0, price: 0 });
      fetchProducts();
      onUpdate();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Erro ao adicionar produto",
      );
      console.error(error);
    } finally {
      setSavingNewItem(false);
    }
  };

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

  const handleEditChange = (
    field: keyof InvoiceProductType,
    value: string | number,
  ) => {
    setEditForm((prev) => ({ ...prev, [field]: value }));
  };

  const saveEdit = async () => {
    if (editingProductId == null) return;
    try {
      setLoading(true);
      const payload = {
        code: editForm.code,
        name: editForm.name,
        price:
          typeof editForm.price === "string"
            ? parseFloat(editForm.price)
            : editForm.price,
        quantity:
          typeof editForm.quantity === "string"
            ? parseFloat(editForm.quantity as string)
            : editForm.quantity,
        unitType: editForm.unitType,
      };
      const updated = await updateInvoiceProduct(editingProductId, payload);
      setProducts((prev) =>
        prev.map((p) => (p.id === updated.id ? updated : p)),
      );
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

  return (
    <div className="fixed inset-0 h-full bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-5xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        <div className="flex justify-between items-center p-6 border-b border-gray-300">
          <h2 className="text-xl font-semibold">
            Produtos da Compra #{purchaseId}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          <div className="flex justify-end mb-4">
            <button
              type="button"
              onClick={() => setAddingItem((prev) => !prev)}
              className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium bg-white text-green-700 border border-green-600 rounded-lg hover:bg-green-50 transition-colors"
            >
              <Plus className="w-4 h-4" />
              Adicionar Item
            </button>
          </div>

          {addingItem && (
            <AddInvoiceItemForm
              catalogProducts={catalogProducts}
              loadingCatalog={loadingCatalog}
              newItem={newItem}
              onCodeChange={(code) => setNewItem((prev) => ({ ...prev, code }))}
              onQuantityChange={(quantity) =>
                setNewItem((prev) => ({ ...prev, quantity }))
              }
              onPriceChange={(price) =>
                setNewItem((prev) => ({ ...prev, price }))
              }
              saving={savingNewItem}
              onSave={handleAddItem}
              onCancel={() => {
                setAddingItem(false);
                setNewItem({ code: "", quantity: 0, price: 0 });
              }}
            />
          )}

          {loading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => (
                <div
                  // biome-ignore lint/suspicious/noArrayIndexKey: static-length skeleton placeholder list, no stable identity available
                  key={i}
                  className="h-16 bg-gray-200 animate-pulse rounded-lg"
                />
              ))}
            </div>
          ) : products.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              Nenhum produto encontrado
            </div>
          ) : (
            <InvoiceProductsTable
              products={products}
              editingProductId={editingProductId}
              editForm={editForm}
              loading={loading}
              onEditChange={handleEditChange}
              onStartEdit={startEdit}
              onCancelEdit={cancelEdit}
              onSaveEdit={saveEdit}
              onDeleteClick={(productId) =>
                setConfirmDeleteModal({ state: true, productId })
              }
            />
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
