"use client";

import {
  CheckCircle2,
  CircleCheck,
  Download,
  RefreshCcw,
  Search,
  Trash2,
  X,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import ConfirmDeleteModal from "@/components/modals/ConfirmDeleteModal";
import { useInvoice } from "@/hooks/useInvoice";
import { combinedScoreService } from "@/services/combinedScoreService";
import type { OpenInvoiceResponse } from "@/types/invoiceType";
import { showError, showSuccess } from "@/utils/toastUtils";
import {
  type BulkActionType,
  DueBadge,
  formatCurrency,
  formatDate,
  goToGrouping,
  InvoiceRowActions,
  type RowActionType,
  triggerDanfeDownload,
} from "./shared";

export default function NfSemBoletoTab() {
  const {
    getOpenInvoiceOnly,
    getDanfe,
    cancelInvoice: cancelInvoiceApi,
  } = useInvoice();

  const [openInvoices, setOpenInvoices] = useState<OpenInvoiceResponse[]>([]);
  const [loadingInvoices, setLoadingInvoices] = useState(true);
  const [invoiceSearchTerm, setInvoiceSearchTerm] = useState("");
  const [selectedInvoiceIds, setSelectedInvoiceIds] = useState<Set<number>>(
    new Set(),
  );
  const [invoiceRowAction, setInvoiceRowAction] = useState<{
    id: number;
    type: RowActionType;
  } | null>(null);
  const [invoiceBulkAction, setInvoiceBulkAction] =
    useState<BulkActionType | null>(null);
  const [cancelInvoiceTarget, setCancelInvoiceTarget] = useState<
    number[] | null
  >(null);
  const [payInvoiceTarget, setPayInvoiceTarget] = useState<number[] | null>(
    null,
  );

  // biome-ignore lint/correctness/useExhaustiveDependencies: getOpenInvoiceOnly is recreated on every render by useInvoice and is not part of the fetch identity
  const fetchOpenInvoices = useCallback(async () => {
    setLoadingInvoices(true);
    try {
      const data = await getOpenInvoiceOnly();
      setOpenInvoices(data);
      setSelectedInvoiceIds(new Set());
    } catch (error) {
      showError("Não foi possível carregar as notas fiscais sem boleto");
      console.error(error);
    } finally {
      setLoadingInvoices(false);
    }
  }, []);

  useEffect(() => {
    fetchOpenInvoices();
  }, [fetchOpenInvoices]);

  const removeInvoices = (ids: number[]) => {
    const idSet = new Set(ids);
    setOpenInvoices((prev) =>
      prev.filter((i) => !idSet.has(i.combinedScoreId)),
    );
    setSelectedInvoiceIds((prev) => {
      const next = new Set(prev);
      ids.forEach((id) => {
        next.delete(id);
      });
      return next;
    });
  };

  const handleConfirmInvoicePayment = (invoice: OpenInvoiceResponse) => {
    setPayInvoiceTarget([invoice.combinedScoreId]);
  };

  const executeConfirmInvoicePayment = async (ids: number[]) => {
    const succeeded: number[] = [];
    const failed: number[] = [];
    for (const id of ids) {
      try {
        await combinedScoreService.confirmPayment(id);
        succeeded.push(id);
      } catch (error) {
        failed.push(id);
        console.error(error);
      }
    }
    if (succeeded.length > 0) {
      removeInvoices(succeeded);
    }
    if (failed.length === 0) {
      showSuccess(
        succeeded.length > 1
          ? `${succeeded.length} pagamento(s) confirmado(s) com sucesso.`
          : "Pagamento confirmado com sucesso.",
      );
    } else if (succeeded.length === 0) {
      showError(
        failed.length > 1
          ? `Não foi possível confirmar o pagamento de ${failed.length} NF(s).`
          : "Não foi possível confirmar o pagamento da NF",
      );
    } else {
      showError(
        `${succeeded.length} pagamento(s) confirmado(s), ${failed.length} falharam.`,
      );
    }
  };

  const confirmPayInvoice = async () => {
    if (!payInvoiceTarget) return;
    const ids = payInvoiceTarget;
    const isSingle = ids.length === 1;
    setPayInvoiceTarget(null);
    if (isSingle) {
      setInvoiceRowAction({ id: ids[0], type: "pay" });
    } else {
      setInvoiceBulkAction("pay");
    }
    try {
      await executeConfirmInvoicePayment(ids);
    } finally {
      setInvoiceRowAction(null);
      setInvoiceBulkAction(null);
    }
  };

  const handleDownloadInvoice = async (invoice: OpenInvoiceResponse) => {
    if (!invoice.invoiceRef) {
      showError("Esta nota fiscal não possui uma referência para download.");
      return;
    }
    setInvoiceRowAction({ id: invoice.combinedScoreId, type: "download" });
    try {
      const blob = await getDanfe(invoice.invoiceRef);
      triggerDanfeDownload(blob, invoice.invoiceRef, invoice.combinedScoreId);
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Não foi possível baixar a NF",
      );
      console.error(error);
    } finally {
      setInvoiceRowAction(null);
    }
  };

  const executeCancelInvoices = async (ids: number[]) => {
    const succeeded: number[] = [];
    const failed: number[] = [];
    for (const id of ids) {
      const invoice = openInvoices.find((i) => i.combinedScoreId === id);
      if (!invoice?.invoiceRef) {
        failed.push(id);
        continue;
      }
      try {
        await cancelInvoiceApi(invoice.invoiceRef);
        succeeded.push(id);
      } catch (error) {
        failed.push(id);
        console.error(error);
      }
    }
    if (succeeded.length > 0) {
      removeInvoices(succeeded);
    }
    if (failed.length === 0) {
      showSuccess(
        succeeded.length > 1
          ? `${succeeded.length} notas fiscais canceladas com sucesso.`
          : "Nota fiscal cancelada com sucesso.",
      );
    } else if (succeeded.length === 0) {
      showError(
        failed.length > 1
          ? `Não foi possível cancelar ${failed.length} notas fiscais.`
          : "Não foi possível cancelar a nota fiscal.",
      );
    } else {
      showError(
        `${succeeded.length} nota(s) fiscal(is) cancelada(s), ${failed.length} falharam.`,
      );
    }
  };

  const handleCancelInvoice = (invoice: OpenInvoiceResponse) => {
    setCancelInvoiceTarget([invoice.combinedScoreId]);
  };

  const confirmCancelInvoice = async () => {
    if (!cancelInvoiceTarget) return;
    const ids = cancelInvoiceTarget;
    const isSingle = ids.length === 1;
    setCancelInvoiceTarget(null);
    if (isSingle) {
      setInvoiceRowAction({ id: ids[0], type: "cancel" });
    } else {
      setInvoiceBulkAction("cancel");
    }
    try {
      await executeCancelInvoices(ids);
    } finally {
      setInvoiceRowAction(null);
      setInvoiceBulkAction(null);
    }
  };

  const filteredOpenInvoices = useMemo(() => {
    if (!invoiceSearchTerm.trim()) return openInvoices;
    const term = invoiceSearchTerm.toLowerCase();
    return openInvoices.filter((i) =>
      i.clientName.toLowerCase().includes(term),
    );
  }, [openInvoices, invoiceSearchTerm]);

  const isInvoiceRowSelected = (id: number) => selectedInvoiceIds.has(id);

  const toggleInvoiceRowSelected = (id: number) => {
    setSelectedInvoiceIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const isAllFilteredInvoicesSelected =
    filteredOpenInvoices.length > 0 &&
    filteredOpenInvoices.every((i) =>
      selectedInvoiceIds.has(i.combinedScoreId),
    );

  const toggleSelectAllFilteredInvoices = () => {
    setSelectedInvoiceIds((prev) => {
      if (isAllFilteredInvoicesSelected) {
        const next = new Set(prev);
        filteredOpenInvoices.forEach((i) => {
          next.delete(i.combinedScoreId);
        });
        return next;
      }
      const next = new Set(prev);
      filteredOpenInvoices.forEach((i) => {
        next.add(i.combinedScoreId);
      });
      return next;
    });
  };

  const clearInvoiceSelection = () => setSelectedInvoiceIds(new Set());

  const selectedInvoices = useMemo(
    () => openInvoices.filter((i) => selectedInvoiceIds.has(i.combinedScoreId)),
    [openInvoices, selectedInvoiceIds],
  );

  const handleBulkConfirmInvoicePayment = () => {
    if (selectedInvoices.length === 0) return;
    setPayInvoiceTarget(selectedInvoices.map((i) => i.combinedScoreId));
  };

  const payInvoiceConfirmTitle = (() => {
    if (!payInvoiceTarget) return "";
    if (payInvoiceTarget.length === 1) {
      const invoice = openInvoices.find(
        (i) => i.combinedScoreId === payInvoiceTarget[0],
      );
      return `Confirmar o pagamento da NF de ${invoice?.clientName ?? "cliente"}${
        invoice ? ` (${formatCurrency(invoice.totalValue)})` : ""
      }? Ele deixará de aparecer na lista de NF sem boleto.`;
    }
    return `Confirmar o pagamento das ${payInvoiceTarget.length} NF selecionadas? Elas deixarão de aparecer na lista de NF sem boleto.`;
  })();

  const handleBulkDownloadInvoices = async () => {
    if (selectedInvoices.length === 0) return;
    setInvoiceBulkAction("download");
    let succeeded = 0;
    let failed = 0;
    for (const invoice of selectedInvoices) {
      if (!invoice.invoiceRef) {
        failed++;
        continue;
      }
      try {
        const blob = await getDanfe(invoice.invoiceRef);
        triggerDanfeDownload(blob, invoice.invoiceRef, invoice.combinedScoreId);
        succeeded++;
      } catch (error) {
        failed++;
        console.error(error);
      }
    }
    if (failed === 0) {
      showSuccess(`${succeeded} NF(s) baixada(s) com sucesso.`);
    } else if (succeeded === 0) {
      showError(
        `Não foi possível baixar nenhuma das ${failed} NF(s) selecionadas.`,
      );
    } else {
      showError(`${succeeded} NF(s) baixada(s), ${failed} falharam.`);
    }
    setInvoiceBulkAction(null);
  };

  const handleBulkCancelInvoices = () => {
    if (selectedInvoices.length === 0) return;
    setCancelInvoiceTarget(selectedInvoices.map((i) => i.combinedScoreId));
  };

  const cancelInvoiceConfirmTitle = (() => {
    if (!cancelInvoiceTarget) return "";
    if (cancelInvoiceTarget.length === 1) {
      const invoice = openInvoices.find(
        (i) => i.combinedScoreId === cancelInvoiceTarget[0],
      );
      return `Tem certeza que deseja cancelar a NF de ${invoice?.clientName ?? "cliente"}${
        invoice ? ` (${formatCurrency(invoice.totalValue)})` : ""
      }? Esta ação cancela a nota fiscal na SEFAZ e não pode ser desfeita.`;
    }
    return `Tem certeza que deseja cancelar as ${cancelInvoiceTarget.length} notas fiscais selecionadas? Esta ação cancela as notas na SEFAZ e não pode ser desfeita.`;
  })();

  return (
    <div>
      <div className="flex flex-wrap justify-between items-center gap-3 mb-5">
        <div>
          <h2 className="text-lg font-semibold text-gray-800">
            Notas fiscais sem boleto vinculado
          </h2>
          <p className="text-sm text-gray-500">
            Clientes que só recebem nota fiscal — consideradas vencidas 20 dias
            após a emissão. Confirme o pagamento manualmente quando o cliente
            quitar.
          </p>
        </div>
        <button
          type="button"
          onClick={fetchOpenInvoices}
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
          value={invoiceSearchTerm}
          onChange={(e) => setInvoiceSearchTerm(e.target.value)}
        />
        <Search className="absolute left-3 top-3 text-gray-400" size={18} />
      </div>

      {selectedInvoiceIds.size > 0 && (
        <div className="flex flex-wrap items-center gap-3 mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
          <span className="text-sm font-medium text-green-900">
            {selectedInvoiceIds.size}{" "}
            {selectedInvoiceIds.size === 1
              ? "NF selecionada"
              : "NF selecionadas"}
          </span>
          <div className="flex flex-wrap items-center gap-2 ml-auto">
            <button
              type="button"
              onClick={handleBulkConfirmInvoicePayment}
              disabled={invoiceBulkAction !== null}
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-green-700 text-white rounded-lg hover:bg-green-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <CheckCircle2 className="w-3 h-3" />
              {invoiceBulkAction === "pay"
                ? "Confirmando..."
                : "Confirmar pagamento"}
            </button>
            <button
              type="button"
              onClick={handleBulkDownloadInvoices}
              disabled={invoiceBulkAction !== null}
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Download className="w-3 h-3" />
              {invoiceBulkAction === "download" ? "Baixando..." : "Baixar NF"}
            </button>
            <button
              type="button"
              onClick={handleBulkCancelInvoices}
              disabled={invoiceBulkAction !== null}
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Trash2 className="w-3 h-3" />
              {invoiceBulkAction === "cancel"
                ? "Processando..."
                : "Cancelar NF"}
            </button>
            <button
              type="button"
              onClick={clearInvoiceSelection}
              disabled={invoiceBulkAction !== null}
              className="inline-flex items-center gap-1 px-3 py-1.5 text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-xs cursor-pointer disabled:opacity-50"
            >
              <X className="w-3 h-3" />
              Limpar seleção
            </button>
          </div>
        </div>
      )}

      {loadingInvoices ? (
        <div className="py-16 text-center">
          <div className="flex justify-center mb-4">
            <div className="h-20 w-20 rounded-full bg-green-50 flex items-center justify-center">
              <div className="animate-spin rounded-full h-14 w-14 border-4 border-gray-100 border-t-green-600 border-r-green-600"></div>
            </div>
          </div>
          <p className="text-lg font-medium text-gray-700">
            Carregando notas fiscais...
          </p>
          <p className="text-sm mt-1 text-gray-500">
            Aguarde enquanto buscamos as notas fiscais pendentes
          </p>
        </div>
      ) : filteredOpenInvoices.length === 0 ? (
        <div className="text-center py-16 text-gray-500">
          <CircleCheck className="w-12 h-12 mx-auto mb-3 text-green-400" />
          <p className="text-lg font-medium text-gray-700">
            {invoiceSearchTerm
              ? "Nenhum cliente encontrado com esse nome"
              : "Nenhuma NF sem boleto pendente no momento"}
          </p>
          {!invoiceSearchTerm && (
            <p className="text-sm mt-1">
              Todos os clientes de NF avulsa estão em dia
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
                      checked={isAllFilteredInvoicesSelected}
                      onChange={toggleSelectAllFilteredInvoices}
                      aria-label="Selecionar todas as NF filtradas"
                    />
                  </th>
                  <th className="py-3 px-3 font-semibold">Cliente</th>
                  <th className="py-3 px-3 font-semibold">Agrupamento</th>
                  <th className="py-3 px-3 font-semibold">Nº da NF</th>
                  <th className="py-3 px-3 font-semibold">Valor</th>
                  <th className="py-3 px-3 font-semibold">Emissão</th>
                  <th className="py-3 px-3 font-semibold">Vencimento</th>
                  <th className="py-3 px-3 font-semibold">Situação</th>
                  <th className="py-3 px-3 font-semibold text-right">Ação</th>
                </tr>
              </thead>
              <tbody>
                {filteredOpenInvoices.map((invoice) => (
                  <tr
                    key={invoice.combinedScoreId}
                    className={`border-b last:border-0 hover:bg-gray-50 transition-colors ${
                      isInvoiceRowSelected(invoice.combinedScoreId)
                        ? "bg-green-50/60"
                        : ""
                    }`}
                  >
                    <td className="py-3 px-3">
                      <input
                        type="checkbox"
                        className="w-4 h-4 cursor-pointer accent-green-700"
                        checked={isInvoiceRowSelected(invoice.combinedScoreId)}
                        onChange={() =>
                          toggleInvoiceRowSelected(invoice.combinedScoreId)
                        }
                        aria-label={`Selecionar NF de ${invoice.clientName}`}
                      />
                    </td>
                    <td className="py-3 px-3 font-medium text-gray-800">
                      {invoice.clientName}
                    </td>
                    <td className="py-3 px-3 text-gray-500">
                      #{invoice.combinedScoreId}
                    </td>
                    <td className="py-3 px-3 text-gray-500">
                      {invoice.invoiceRef || "—"}
                    </td>
                    <td className="py-3 px-3">
                      {formatCurrency(invoice.totalValue)}
                    </td>
                    <td className="py-3 px-3">
                      {formatDate(invoice.confirmedAt)}
                    </td>
                    <td className="py-3 px-3">{formatDate(invoice.dueDate)}</td>
                    <td className="py-3 px-3">
                      <DueBadge dueDate={invoice.dueDate} />
                    </td>
                    <td className="py-3 px-3 text-right">
                      <div className="flex items-center justify-end">
                        <InvoiceRowActions
                          invoice={invoice}
                          rowAction={invoiceRowAction}
                          bulkAction={invoiceBulkAction}
                          onConfirmPayment={handleConfirmInvoicePayment}
                          onDownload={handleDownloadInvoice}
                          onCancel={handleCancelInvoice}
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
            {filteredOpenInvoices.map((invoice) => (
              <div
                key={invoice.combinedScoreId}
                className={`border rounded-lg p-4 ${
                  isInvoiceRowSelected(invoice.combinedScoreId)
                    ? "border-green-300 bg-green-50/60"
                    : "border-gray-200"
                }`}
              >
                <div className="flex items-start gap-3">
                  <input
                    type="checkbox"
                    className="w-4 h-4 mt-1 cursor-pointer accent-green-700 shrink-0"
                    checked={isInvoiceRowSelected(invoice.combinedScoreId)}
                    onChange={() =>
                      toggleInvoiceRowSelected(invoice.combinedScoreId)
                    }
                    aria-label={`Selecionar NF de ${invoice.clientName}`}
                  />
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-800 break-words">
                      {invoice.clientName}
                    </p>
                    <p className="text-xs text-gray-500 mt-0.5">
                      Agrupamento #{invoice.combinedScoreId}
                      {invoice.invoiceRef ? ` · NF ${invoice.invoiceRef}` : ""}
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 mt-3 text-sm">
                  <div>
                    <span className="block text-xs text-gray-500">Valor</span>
                    <span className="font-medium text-gray-800">
                      {formatCurrency(invoice.totalValue)}
                    </span>
                  </div>
                  <div>
                    <span className="block text-xs text-gray-500">
                      Vencimento
                    </span>
                    <span className="font-medium text-gray-800">
                      {formatDate(invoice.dueDate)}
                    </span>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-1.5 mt-3">
                  <DueBadge dueDate={invoice.dueDate} />
                </div>

                <div className="flex items-center justify-end mt-3 pt-3 border-t border-gray-100">
                  <InvoiceRowActions
                    invoice={invoice}
                    rowAction={invoiceRowAction}
                    bulkAction={invoiceBulkAction}
                    onConfirmPayment={handleConfirmInvoicePayment}
                    onDownload={handleDownloadInvoice}
                    onCancel={handleCancelInvoice}
                    onViewGrouping={goToGrouping}
                  />
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      <ConfirmDeleteModal
        open={cancelInvoiceTarget !== null}
        onClose={() => setCancelInvoiceTarget(null)}
        onConfirm={confirmCancelInvoice}
        title={cancelInvoiceConfirmTitle}
      />

      <ConfirmDeleteModal
        open={payInvoiceTarget !== null}
        onClose={() => setPayInvoiceTarget(null)}
        onConfirm={confirmPayInvoice}
        title={payInvoiceConfirmTitle}
      />
    </div>
  );
}
