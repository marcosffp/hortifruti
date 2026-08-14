"use client";

import { Plus } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import ConfirmDeleteModal from "@/components/modals/ConfirmDeleteModal";
import CreateManualPurchaseModal from "@/components/modals/CreateManualPurchaseModal";
import CriarAgrupamentoModal from "@/components/modals/CriarAgrupamentoModal";
import InvoiceProductsModal from "@/components/modals/InvoiceProductsModal";
import { usePurchase } from "@/hooks/usePurchase";
import type { PurchaseType } from "@/types/purchaseType";
import { showError, showSuccess } from "@/utils/toastUtils";
import {
  PurchaseFileCard,
  PurchaseFileTableRow,
} from "./purchase-files-table/PurchaseFileRows";

interface PurchaseFilesTableProps {
  clientId?: number;
  refreshKey?: number;
  onGroupingCreated?: () => void;
}

export default function PurchaseFilesTable({
  clientId,
  refreshKey,
  onGroupingCreated,
}: PurchaseFilesTableProps) {
  const [purchases, setPurchases] = useState<PurchaseType[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedPurchase, setSelectedPurchase] = useState<PurchaseType | null>(
    null,
  );
  const [showModal, setShowModal] = useState(false);
  const [showGroupingModal, setShowGroupingModal] = useState(false);
  const [showManualPurchaseModal, setShowManualPurchaseModal] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const debounceTimer = useRef<NodeJS.Timeout | null>(null);
  const { fetchPurchaseFiles, deletePurchaseFile } = usePurchase();

  const fetchPurchases = useCallback(async () => {
    if (!clientId) {
      setPurchases([]);
      return;
    }

    setLoading(true);
    try {
      const data = await fetchPurchaseFiles(clientId, page, 10);
      setPurchases(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (error) {
      showError("Erro ao carregar arquivos de compra");
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [clientId, page, fetchPurchaseFiles]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: refreshKey is intentionally unused inside the effect — it only exists to force a refetch when the parent bumps it
  useEffect(() => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current);

    debounceTimer.current = setTimeout(() => {
      fetchPurchases();
    }, 300);

    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [fetchPurchases, refreshKey]);

  const handleDelete = (purchaseId: number) => {
    setDeleteTarget(purchaseId);
  };

  const confirmDeletePurchase = async () => {
    if (deleteTarget === null) return;
    const purchaseId = deleteTarget;
    setDeleteTarget(null);
    try {
      await deletePurchaseFile(purchaseId);
      showSuccess("Arquivo deletado com sucesso");
      fetchPurchases();
    } catch (error) {
      showError("Erro ao deletar arquivo");
      console.error(error);
    }
  };

  const handleViewProducts = (purchase: PurchaseType) => {
    setSelectedPurchase(purchase);
    setShowModal(true);
  };

  const formatDate = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleDateString("pt-BR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateString;
    }
  };

  if (!clientId) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p>Selecione um cliente para visualizar os arquivos de compra</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header com título e botão */}
      <div className="flex justify-between items-center gap-2 flex-wrap">
        <h2 className="text-lg font-semibold">Arquivos de Compra</h2>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setShowManualPurchaseModal(true)}
            disabled={!clientId}
            className={`flex items-center gap-2 px-4 py-2 bg-white text-green-700 border border-green-600 rounded-lg hover:bg-green-50 transition-colors ${
              !clientId ? "opacity-50 cursor-not-allowed" : ""
            }`}
          >
            <Plus className="w-4 h-4" />
            Criar Compra
          </button>
          <button
            type="button"
            onClick={() => setShowGroupingModal(true)}
            disabled={!clientId || purchases.length === 0}
            className={`flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors ${
              !clientId || purchases.length === 0
                ? "opacity-50 cursor-not-allowed"
                : ""
            }`}
          >
            <Plus className="w-4 h-4" />
            Criar Agrupamento
          </button>
        </div>
      </div>

      {/* Loading skeleton */}
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
      ) : purchases.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          <p>Nenhum arquivo de compra encontrado</p>
        </div>
      ) : (
        <>
          {/* Tabela (desktop) */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="bg-gray-100 border-b border-gray-300">
                  <th className="text-left p-3 font-semibold">
                    Data da Compra
                  </th>
                  <th className="text-left p-3 font-semibold">Valor Total</th>
                  <th className="text-left p-3 font-semibold">
                    Última Atualização
                  </th>
                  <th className="text-center p-3 font-semibold">Ações</th>
                </tr>
              </thead>
              <tbody>
                {purchases.map((purchase) => (
                  <PurchaseFileTableRow
                    key={purchase.id}
                    purchase={purchase}
                    formatDate={formatDate}
                    onViewProducts={handleViewProducts}
                    onDelete={handleDelete}
                  />
                ))}
              </tbody>
            </table>
          </div>

          {/* Cards (mobile) */}
          <div className="md:hidden space-y-3">
            {purchases.map((purchase) => (
              <PurchaseFileCard
                key={purchase.id}
                purchase={purchase}
                formatDate={formatDate}
                onViewProducts={handleViewProducts}
                onDelete={handleDelete}
              />
            ))}
          </div>

          {/* Paginação */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-4">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Anterior
              </button>
              <span className="text-sm">
                Página {page + 1} de {totalPages}
              </span>
              <button
                type="button"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Próxima
              </button>
            </div>
          )}
        </>
      )}

      {/* Modal de Criar Agrupamento */}
      {showGroupingModal && clientId && (
        <CriarAgrupamentoModal
          clientId={clientId}
          onClose={() => setShowGroupingModal(false)}
          onCreated={onGroupingCreated}
        />
      )}

      {/* Modal de criação manual de compra */}
      {showManualPurchaseModal && clientId && (
        <CreateManualPurchaseModal
          clientId={clientId}
          onClose={() => setShowManualPurchaseModal(false)}
          onCreated={fetchPurchases}
        />
      )}

      {/* Modal de produtos */}
      {showModal && selectedPurchase && (
        <InvoiceProductsModal
          purchaseId={selectedPurchase.id}
          onClose={() => {
            setShowModal(false);
            setSelectedPurchase(null);
          }}
          onUpdate={fetchPurchases}
        />
      )}

      <ConfirmDeleteModal
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDeletePurchase}
        title="Tem certeza que deseja deletar este arquivo de compra?"
      />
    </div>
  );
}
