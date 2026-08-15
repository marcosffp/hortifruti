"use client";

import {
  CheckCircle2,
  CircleCheck,
  Download,
  RefreshCcw,
  Search,
  ShieldQuestion,
  Trash2,
  X,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import ConfirmDeleteModal from "@/components/modals/ConfirmDeleteModal";
import { useBillet } from "@/hooks/useBillet";
import type { OpenBilletResponse } from "@/types/billetType";
import { showError, showSuccess } from "@/utils/toastUtils";
import {
  BilletRowActions,
  type BulkActionType,
  DueBadge,
  executeBulkAction,
  formatCurrency,
  formatDate,
  goToGrouping,
  type RowActionType,
  triggerPdfDownload,
} from "./shared";

export default function BoletosAbertosTab() {
  const {
    getOpenBillets,
    markBilletAsPaid,
    downloadStoredBillet,
    cancelBillet,
  } = useBillet();

  const [openBillets, setOpenBillets] = useState<OpenBilletResponse[]>([]);
  const [loadingOpen, setLoadingOpen] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [rowAction, setRowAction] = useState<{
    id: number;
    type: RowActionType;
  } | null>(null);
  const [bulkAction, setBulkAction] = useState<BulkActionType | null>(null);
  const [cancelTarget, setCancelTarget] = useState<number[] | null>(null);
  const [payTarget, setPayTarget] = useState<number[] | null>(null);

  // biome-ignore lint/correctness/useExhaustiveDependencies: getOpenBillets is recreated on every render by useBillet and is not part of the fetch identity
  const fetchOpenBillets = useCallback(async () => {
    setLoadingOpen(true);
    try {
      const data = await getOpenBillets();
      setOpenBillets(data);
      setSelectedIds(new Set());
    } catch (error) {
      showError("Não foi possível carregar os boletos em aberto");
      console.error(error);
    } finally {
      setLoadingOpen(false);
    }
  }, []);

  useEffect(() => {
    fetchOpenBillets();
  }, [fetchOpenBillets]);

  const removeBillets = (ids: number[]) => {
    const idSet = new Set(ids);
    setOpenBillets((prev) => prev.filter((b) => !idSet.has(b.combinedScoreId)));
    setSelectedIds((prev) => {
      const next = new Set(prev);
      ids.forEach((id) => {
        next.delete(id);
      });
      return next;
    });
  };

  const handleMarkAsPaid = (billet: OpenBilletResponse) => {
    setPayTarget([billet.combinedScoreId]);
  };

  const executeMarkAsPaid = async (ids: number[]) => {
    const { succeeded } = await executeBulkAction(ids, markBilletAsPaid, {
      successOne: "Pagamento confirmado com sucesso.",
      successMany: (count) =>
        `${count} pagamento(s) confirmado(s) com sucesso.`,
      failureOne: "Não foi possível confirmar o pagamento do boleto",
      failureMany: (count) =>
        `Não foi possível confirmar o pagamento de ${count} boleto(s).`,
      partial: (succeeded, failed) =>
        `${succeeded} pagamento(s) confirmado(s), ${failed} falharam.`,
    });
    if (succeeded.length > 0) {
      removeBillets(succeeded);
    }
  };

  const confirmPayBillet = async () => {
    if (!payTarget) return;
    const ids = payTarget;
    const isSingle = ids.length === 1;
    setPayTarget(null);
    if (isSingle) {
      setRowAction({ id: ids[0], type: "pay" });
    } else {
      setBulkAction("pay");
    }
    try {
      await executeMarkAsPaid(ids);
    } finally {
      setRowAction(null);
      setBulkAction(null);
    }
  };

  const handleDownloadPdf = async (billet: OpenBilletResponse) => {
    setRowAction({ id: billet.combinedScoreId, type: "download" });
    try {
      const blob = await downloadStoredBillet(billet.combinedScoreId);
      triggerPdfDownload(blob, billet.yourNumber, billet.combinedScoreId);
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Não foi possível baixar o PDF do boleto",
      );
      console.error(error);
    } finally {
      setRowAction(null);
    }
  };

  const executeCancel = async (ids: number[]) => {
    const { succeeded } = await executeBulkAction(ids, cancelBillet, {
      successOne: "Boleto com baixa realizada com sucesso.",
      successMany: (count) =>
        `${count} boletos com baixa realizada com sucesso.`,
      failureOne: "Não foi possível dar baixa no boleto.",
      failureMany: (count) => `Não foi possível dar baixa em ${count} boletos.`,
      partial: (succeeded, failed) =>
        `${succeeded} boleto(s) com baixa realizada, ${failed} falharam.`,
    });
    if (succeeded.length > 0) {
      removeBillets(succeeded);
    }
  };

  const handleCancelBillet = (billet: OpenBilletResponse) => {
    setCancelTarget([billet.combinedScoreId]);
  };

  const confirmCancel = async () => {
    if (!cancelTarget) return;
    const ids = cancelTarget;
    const isSingle = ids.length === 1;
    setCancelTarget(null);
    if (isSingle) {
      setRowAction({ id: ids[0], type: "cancel" });
    } else {
      setBulkAction("cancel");
    }
    try {
      await executeCancel(ids);
    } finally {
      setRowAction(null);
      setBulkAction(null);
    }
  };

  const filteredOpenBillets = useMemo(() => {
    if (!searchTerm.trim()) return openBillets;
    const term = searchTerm.toLowerCase();
    return openBillets.filter((b) => b.clientName.toLowerCase().includes(term));
  }, [openBillets, searchTerm]);

  const isRowSelected = (id: number) => selectedIds.has(id);

  const toggleRowSelected = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const isAllFilteredSelected =
    filteredOpenBillets.length > 0 &&
    filteredOpenBillets.every((b) => selectedIds.has(b.combinedScoreId));

  const toggleSelectAllFiltered = () => {
    setSelectedIds((prev) => {
      if (isAllFilteredSelected) {
        const next = new Set(prev);
        filteredOpenBillets.forEach((b) => {
          next.delete(b.combinedScoreId);
        });
        return next;
      }
      const next = new Set(prev);
      filteredOpenBillets.forEach((b) => {
        next.add(b.combinedScoreId);
      });
      return next;
    });
  };

  const clearSelection = () => setSelectedIds(new Set());

  const selectedBillets = useMemo(
    () => openBillets.filter((b) => selectedIds.has(b.combinedScoreId)),
    [openBillets, selectedIds],
  );

  const handleBulkMarkAsPaid = () => {
    if (selectedBillets.length === 0) return;
    setPayTarget(selectedBillets.map((b) => b.combinedScoreId));
  };

  const payConfirmTitle = (() => {
    if (!payTarget) return "";
    if (payTarget.length === 1) {
      const billet = openBillets.find(
        (b) => b.combinedScoreId === payTarget[0],
      );
      return `Confirmar o pagamento do boleto de ${billet?.clientName ?? "cliente"}${
        billet ? ` (${formatCurrency(billet.totalValue)})` : ""
      }? Ele deixará de aparecer na lista de boletos em aberto.`;
    }
    return `Confirmar o pagamento dos ${payTarget.length} boletos selecionados? Eles deixarão de aparecer na lista de boletos em aberto.`;
  })();

  const handleBulkDownload = async () => {
    if (selectedBillets.length === 0) return;
    setBulkAction("download");
    let succeeded = 0;
    let failed = 0;
    for (const billet of selectedBillets) {
      try {
        const blob = await downloadStoredBillet(billet.combinedScoreId);
        triggerPdfDownload(blob, billet.yourNumber, billet.combinedScoreId);
        succeeded++;
      } catch (error) {
        failed++;
        console.error(error);
      }
    }
    if (failed === 0) {
      showSuccess(`${succeeded} PDF(s) baixado(s) com sucesso.`);
    } else if (succeeded === 0) {
      showError(
        `Não foi possível baixar nenhum dos ${failed} PDF(s) selecionados.`,
      );
    } else {
      showError(`${succeeded} PDF(s) baixado(s), ${failed} falharam.`);
    }
    setBulkAction(null);
  };

  const handleBulkCancel = () => {
    if (selectedBillets.length === 0) return;
    setCancelTarget(selectedBillets.map((b) => b.combinedScoreId));
  };

  const cancelConfirmTitle = (() => {
    if (!cancelTarget) return "";
    if (cancelTarget.length === 1) {
      const billet = openBillets.find(
        (b) => b.combinedScoreId === cancelTarget[0],
      );
      return `Tem certeza que deseja dar baixa no boleto de ${billet?.clientName ?? "cliente"}${
        billet ? ` (${formatCurrency(billet.totalValue)})` : ""
      }? Esta ação não pode ser desfeita.`;
    }
    return `Tem certeza que deseja dar baixa nos ${cancelTarget.length} boletos selecionados? Esta ação não pode ser desfeita.`;
  })();

  return (
    <div>
      <div className="flex flex-wrap justify-between items-center gap-3 mb-5">
        <div>
          <h2 className="text-lg font-semibold text-gray-800">
            Todos os boletos em aberto
          </h2>
          <p className="text-sm text-gray-500">
            Ordenados do vencimento mais próximo para o mais distante
          </p>
        </div>
        <button
          type="button"
          onClick={fetchOpenBillets}
          className="flex items-center gap-2 px-3 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors cursor-pointer"
        >
          <RefreshCcw className="w-4 h-4" />
          Atualizar
        </button>
      </div>

      <div className="relative w-full max-w-md mb-5">
        <input
          type="text"
          placeholder="Buscar por nome do cliente..."
          className="pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg w-full focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 transition-all"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
        <Search className="absolute left-3 top-3 text-gray-400" size={18} />
      </div>

      {selectedIds.size > 0 && (
        <div className="flex flex-wrap items-center gap-3 mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
          <span className="text-sm font-medium text-green-900">
            {selectedIds.size}{" "}
            {selectedIds.size === 1
              ? "boleto selecionado"
              : "boletos selecionados"}
          </span>
          <div className="flex flex-wrap items-center gap-2 ml-auto">
            <button
              type="button"
              onClick={handleBulkMarkAsPaid}
              disabled={bulkAction !== null}
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-green-700 text-white rounded-lg hover:bg-green-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <CheckCircle2 className="w-3 h-3" />
              {bulkAction === "pay" ? "Confirmando..." : "Marcar como pago"}
            </button>
            <button
              type="button"
              onClick={handleBulkDownload}
              disabled={bulkAction !== null}
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Download className="w-3 h-3" />
              {bulkAction === "download" ? "Baixando..." : "Baixar PDF"}
            </button>
            <button
              type="button"
              onClick={handleBulkCancel}
              disabled={bulkAction !== null}
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Trash2 className="w-3 h-3" />
              {bulkAction === "cancel" ? "Processando..." : "Dar baixa"}
            </button>
            <button
              type="button"
              onClick={clearSelection}
              disabled={bulkAction !== null}
              className="inline-flex items-center gap-1 px-3 py-1.5 text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-xs cursor-pointer disabled:opacity-50"
            >
              <X className="w-3 h-3" />
              Limpar seleção
            </button>
          </div>
        </div>
      )}

      {loadingOpen ? (
        <div className="py-16 text-center">
          <div className="flex justify-center mb-4">
            <div className="h-20 w-20 rounded-full bg-green-50 flex items-center justify-center">
              <div className="animate-spin rounded-full h-14 w-14 border-4 border-gray-100 border-t-green-600 border-r-green-600"></div>
            </div>
          </div>
          <p className="text-lg font-medium text-gray-700">
            Carregando boletos em aberto...
          </p>
          <p className="text-sm mt-1 text-gray-500">
            Aguarde enquanto buscamos os dados dos boletos
          </p>
        </div>
      ) : filteredOpenBillets.length === 0 ? (
        <div className="text-center py-16 text-gray-500">
          <CircleCheck className="w-12 h-12 mx-auto mb-3 text-green-400" />
          <p className="text-lg font-medium text-gray-700">
            {searchTerm
              ? "Nenhum cliente encontrado com esse nome"
              : "Nenhum boleto em aberto no momento"}
          </p>
          {!searchTerm && (
            <p className="text-sm mt-1">
              Todos os clientes estão em dia com os pagamentos
            </p>
          )}
        </div>
      ) : (
        <>
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500 border-b">
                  <th className="py-3 px-3 font-semibold w-10">
                    <input
                      type="checkbox"
                      className="w-4 h-4 cursor-pointer accent-green-700"
                      checked={isAllFilteredSelected}
                      onChange={toggleSelectAllFiltered}
                      aria-label="Selecionar todos os boletos filtrados"
                    />
                  </th>
                  <th className="py-3 px-3 font-semibold">Cliente</th>
                  <th className="py-3 px-3 font-semibold">Agrupamento</th>
                  <th className="py-3 px-3 font-semibold">Nº do Boleto</th>
                  <th className="py-3 px-3 font-semibold">Valor</th>
                  <th className="py-3 px-3 font-semibold">Vencimento</th>
                  <th className="py-3 px-3 font-semibold">Situação</th>
                  <th className="py-3 px-3 font-semibold text-right">Ação</th>
                </tr>
              </thead>
              <tbody>
                {filteredOpenBillets.map((billet) => (
                  <tr
                    key={billet.combinedScoreId}
                    className={`border-b last:border-0 hover:bg-gray-50 transition-colors ${
                      isRowSelected(billet.combinedScoreId)
                        ? "bg-green-50/60"
                        : ""
                    }`}
                  >
                    <td className="py-3 px-3">
                      <input
                        type="checkbox"
                        className="w-4 h-4 cursor-pointer accent-green-700"
                        checked={isRowSelected(billet.combinedScoreId)}
                        onChange={() =>
                          toggleRowSelected(billet.combinedScoreId)
                        }
                        aria-label={`Selecionar boleto de ${billet.clientName}`}
                      />
                    </td>
                    <td className="py-3 px-3 font-medium text-gray-800">
                      {billet.clientName}
                    </td>
                    <td className="py-3 px-3 text-gray-500">
                      #{billet.combinedScoreId}
                    </td>
                    <td className="py-3 px-3 text-gray-500">
                      {billet.yourNumber || "—"}
                    </td>
                    <td className="py-3 px-3">
                      {formatCurrency(billet.totalValue)}
                    </td>
                    <td className="py-3 px-3">{formatDate(billet.dueDate)}</td>
                    <td className="py-3 px-3">
                      <div className="flex items-center gap-1.5">
                        <DueBadge dueDate={billet.dueDate} />
                        {!billet.confirmadoNoSicoob && (
                          <span
                            title="Não foi possível confirmar essa situação diretamente com o Sicoob agora. Os dados podem estar desatualizados."
                            className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-yellow-100 text-yellow-800 cursor-help"
                          >
                            <ShieldQuestion className="w-3 h-3" />
                            Não confirmado
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-3 text-right">
                      <div className="flex items-center justify-end">
                        <BilletRowActions
                          billet={billet}
                          rowAction={rowAction}
                          bulkAction={bulkAction}
                          onMarkAsPaid={handleMarkAsPaid}
                          onDownload={handleDownloadPdf}
                          onCancel={handleCancelBillet}
                          onViewGrouping={goToGrouping}
                        />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="md:hidden space-y-3">
            {filteredOpenBillets.map((billet) => (
              <div
                key={billet.combinedScoreId}
                className={`border rounded-lg p-4 ${
                  isRowSelected(billet.combinedScoreId)
                    ? "border-green-300 bg-green-50/60"
                    : "border-gray-200"
                }`}
              >
                <div className="flex items-start gap-3">
                  <input
                    type="checkbox"
                    className="w-4 h-4 mt-1 cursor-pointer accent-green-700 shrink-0"
                    checked={isRowSelected(billet.combinedScoreId)}
                    onChange={() => toggleRowSelected(billet.combinedScoreId)}
                    aria-label={`Selecionar boleto de ${billet.clientName}`}
                  />
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-800 break-words">
                      {billet.clientName}
                    </p>
                    <p className="text-xs text-gray-500 mt-0.5">
                      Agrupamento #{billet.combinedScoreId}
                      {billet.yourNumber
                        ? ` · Boleto ${billet.yourNumber}`
                        : ""}
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 mt-3 text-sm">
                  <div>
                    <span className="block text-xs text-gray-500">Valor</span>
                    <span className="font-medium text-gray-800">
                      {formatCurrency(billet.totalValue)}
                    </span>
                  </div>
                  <div>
                    <span className="block text-xs text-gray-500">
                      Vencimento
                    </span>
                    <span className="font-medium text-gray-800">
                      {formatDate(billet.dueDate)}
                    </span>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-1.5 mt-3">
                  <DueBadge dueDate={billet.dueDate} />
                  {!billet.confirmadoNoSicoob && (
                    <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-yellow-100 text-yellow-800">
                      <ShieldQuestion className="w-3 h-3" />
                      Não confirmado
                    </span>
                  )}
                </div>

                <div className="flex items-center justify-end mt-3 pt-3 border-t border-gray-100">
                  <BilletRowActions
                    billet={billet}
                    rowAction={rowAction}
                    bulkAction={bulkAction}
                    onMarkAsPaid={handleMarkAsPaid}
                    onDownload={handleDownloadPdf}
                    onCancel={handleCancelBillet}
                    onViewGrouping={goToGrouping}
                  />
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      <ConfirmDeleteModal
        open={cancelTarget !== null}
        onClose={() => setCancelTarget(null)}
        onConfirm={confirmCancel}
        title={cancelConfirmTitle}
      />

      <ConfirmDeleteModal
        open={payTarget !== null}
        onClose={() => setPayTarget(null)}
        onConfirm={confirmPayBillet}
        title={payConfirmTitle}
      />
    </div>
  );
}
