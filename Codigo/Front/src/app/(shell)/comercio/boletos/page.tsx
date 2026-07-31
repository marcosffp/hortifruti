"use client";

import {
  AlertCircle,
  AlertTriangle,
  CalendarClock,
  CheckCircle2,
  CircleCheck,
  Clock,
  Download,
  ExternalLink,
  FileSearch,
  FileText,
  Filter,
  ListChecks,
  Receipt,
  RefreshCcw,
  Search,
  ShieldQuestion,
  Trash2,
  X,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import ConfirmDeleteModal from "@/components/modals/ConfirmDeleteModal";
import ManualCancelModal from "@/components/modals/ManualCancelModal";
import ClientSelector from "@/components/modules/ClientSelector";
import { useBillet } from "@/hooks/useBillet";
import { useInvoice } from "@/hooks/useInvoice";
import { combinedScoreService } from "@/services/combinedScoreService";
import { showError, showSuccess } from "@/services/notificationService";
import type { BilletResponse, OpenBilletResponse } from "@/types/billetType";
import type { ClientSelectionInfo } from "@/types/clientType";
import type { OpenInvoiceResponse } from "@/types/invoiceType";

type RowActionType = "pay" | "download" | "cancel";
type BulkActionType = "pay" | "download" | "cancel";

function triggerPdfDownload(
  blob: Blob,
  yourNumber: string | null,
  combinedScoreId: number,
) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", `BOL-${yourNumber || combinedScoreId}.pdf`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

function triggerDanfeDownload(
  blob: Blob,
  invoiceRef: string | null,
  combinedScoreId: number,
) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", `NF-${invoiceRef || combinedScoreId}.pdf`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

type Tab = "abertos" | "porCliente" | "nfSemBoleto";

const SITUACAO_OPTIONS = [
  { value: "", label: "Todas as situações" },
  { value: "1", label: "Em aberto" },
  { value: "2", label: "Baixado" },
  { value: "3", label: "Liquidado" },
];

function formatCurrency(value: number | null | undefined): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value || 0);
}

function formatDate(dateString: string | null | undefined): string {
  if (!dateString) return "Não definida";
  try {
    const datePart = dateString.split("T")[0];
    const [year, month, day] = datePart.split("-");
    if (!year || !month || !day) return dateString;
    return `${day}/${month}/${year}`;
  } catch {
    return dateString;
  }
}

function daysUntil(dateString: string | null | undefined): number | null {
  if (!dateString) return null;
  const datePart = dateString.split("T")[0];
  const due = new Date(`${datePart}T00:00:00`);
  if (Number.isNaN(due.getTime())) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const diffMs = due.getTime() - today.getTime();
  return Math.round(diffMs / (1000 * 60 * 60 * 24));
}

function DueBadge({ dueDate }: { dueDate: string | null | undefined }) {
  const diff = daysUntil(dueDate);

  if (diff === null) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-700">
        Sem vencimento
      </span>
    );
  }

  if (diff < 0) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-red-100 text-red-800">
        <AlertCircle className="w-3 h-3" />
        Vencido há {Math.abs(diff)} {Math.abs(diff) === 1 ? "dia" : "dias"}
      </span>
    );
  }

  if (diff === 0) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-orange-100 text-orange-800">
        <Clock className="w-3 h-3" />
        Vence hoje
      </span>
    );
  }

  return (
    <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-blue-100 text-blue-800">
      <CalendarClock className="w-3 h-3" />
      Vence em {diff} {diff === 1 ? "dia" : "dias"}
    </span>
  );
}

function situacaoBadgeColor(situacao: string): string {
  const value = situacao.toLowerCase();
  if (value.includes("liquidado") || value.includes("pago")) {
    return "bg-green-100 text-green-800";
  }
  if (value.includes("baixado") || value.includes("cancelado")) {
    return "bg-red-100 text-red-800";
  }
  return "bg-blue-100 text-blue-800";
}

function isBilletCancelable(situacao: string): boolean {
  const value = situacao.toLowerCase();
  return (
    !value.includes("liquidado") &&
    !value.includes("pago") &&
    !value.includes("baixado") &&
    !value.includes("cancelado")
  );
}

function getClientBilletKey(billet: BilletResponse): string {
  return `${billet.seuNumero}-${billet.dataVencimento}-${billet.valor}`;
}

function ActionSpinner() {
  return (
    <span className="block w-4 h-4 border-2 border-white/60 border-t-white rounded-full animate-spin" />
  );
}

function BilletRowActions({
  billet,
  rowAction,
  bulkAction,
  onMarkAsPaid,
  onDownload,
  onCancel,
  onViewGrouping,
}: {
  billet: OpenBilletResponse;
  rowAction: { id: number; type: RowActionType } | null;
  bulkAction: BulkActionType | null;
  onMarkAsPaid: (billet: OpenBilletResponse) => void;
  onDownload: (billet: OpenBilletResponse) => void;
  onCancel: (billet: OpenBilletResponse) => void;
  onViewGrouping: (clientId: number) => void;
}) {
  const disabled = rowAction !== null || bulkAction !== null;
  const isActing = (type: RowActionType) =>
    rowAction?.id === billet.combinedScoreId && rowAction.type === type;

  return (
    <div className="flex items-center gap-1.5">
      <button
        type="button"
        onClick={() => onMarkAsPaid(billet)}
        disabled={disabled}
        title="Marcar como pago"
        aria-label="Marcar como pago"
        className="p-2 bg-green-700 text-white rounded-lg hover:bg-green-800 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isActing("pay") ? (
          <ActionSpinner />
        ) : (
          <CheckCircle2 className="w-4 h-4" />
        )}
      </button>
      <button
        type="button"
        onClick={() => onDownload(billet)}
        disabled={disabled}
        title="Baixar PDF"
        aria-label="Baixar PDF"
        className="p-2 bg-gray-700 text-white rounded-lg hover:bg-gray-800 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isActing("download") ? (
          <ActionSpinner />
        ) : (
          <Download className="w-4 h-4" />
        )}
      </button>
      <button
        type="button"
        onClick={() => onCancel(billet)}
        disabled={disabled}
        title="Dar baixa"
        aria-label="Dar baixa"
        className="p-2 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isActing("cancel") ? (
          <ActionSpinner />
        ) : (
          <Trash2 className="w-4 h-4" />
        )}
      </button>
      <button
        type="button"
        onClick={() => onViewGrouping(billet.clientId)}
        title="Ver Agrupamento"
        aria-label="Ver Agrupamento"
        className="p-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors cursor-pointer"
      >
        <ExternalLink className="w-4 h-4" />
      </button>
    </div>
  );
}

function InvoiceRowActions({
  invoice,
  rowAction,
  bulkAction,
  onConfirmPayment,
  onDownload,
  onCancel,
  onViewGrouping,
}: {
  invoice: OpenInvoiceResponse;
  rowAction: { id: number; type: RowActionType } | null;
  bulkAction: BulkActionType | null;
  onConfirmPayment: (invoice: OpenInvoiceResponse) => void;
  onDownload: (invoice: OpenInvoiceResponse) => void;
  onCancel: (invoice: OpenInvoiceResponse) => void;
  onViewGrouping: (clientId: number) => void;
}) {
  const disabled = rowAction !== null || bulkAction !== null;
  const isActing = (type: RowActionType) =>
    rowAction?.id === invoice.combinedScoreId && rowAction.type === type;

  return (
    <div className="flex items-center gap-1.5">
      <button
        type="button"
        onClick={() => onConfirmPayment(invoice)}
        disabled={disabled}
        title="Confirmar pagamento"
        aria-label="Confirmar pagamento"
        className="p-2 bg-green-700 text-white rounded-lg hover:bg-green-800 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isActing("pay") ? (
          <ActionSpinner />
        ) : (
          <CheckCircle2 className="w-4 h-4" />
        )}
      </button>
      <button
        type="button"
        onClick={() => onDownload(invoice)}
        disabled={disabled}
        title="Baixar NF (DANFE)"
        aria-label="Baixar NF (DANFE)"
        className="p-2 bg-gray-700 text-white rounded-lg hover:bg-gray-800 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isActing("download") ? (
          <ActionSpinner />
        ) : (
          <Download className="w-4 h-4" />
        )}
      </button>
      <button
        type="button"
        onClick={() => onCancel(invoice)}
        disabled={disabled}
        title="Cancelar nota fiscal"
        aria-label="Cancelar nota fiscal"
        className="p-2 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isActing("cancel") ? (
          <ActionSpinner />
        ) : (
          <Trash2 className="w-4 h-4" />
        )}
      </button>
      <button
        type="button"
        onClick={() => onViewGrouping(invoice.clientId)}
        title="Ver Agrupamento"
        aria-label="Ver Agrupamento"
        className="p-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors cursor-pointer"
      >
        <ExternalLink className="w-4 h-4" />
      </button>
    </div>
  );
}

export default function BoletosPage() {
  const {
    getOpenBillets,
    getClientBillets,
    markBilletAsPaid,
    downloadStoredBillet,
    cancelBillet,
    cancelBilletByNumber,
    isLoading,
  } = useBillet();

  const {
    getOpenInvoiceOnly,
    getDanfe,
    cancelInvoice: cancelInvoiceApi,
  } = useInvoice();

  const [tab, setTab] = useState<Tab>("abertos");

  // Aba "Em aberto"
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
  const [showManualCancelModal, setShowManualCancelModal] = useState(false);

  // Aba "Consultar por cliente"
  const [selectedClient, setSelectedClient] =
    useState<ClientSelectionInfo | null>(null);
  const [situacao, setSituacao] = useState("");
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");
  const [clientBillets, setClientBillets] = useState<BilletResponse[] | null>(
    null,
  );
  const [loadingClientBillets, setLoadingClientBillets] = useState(false);
  const [clientBilletActionKey, setClientBilletActionKey] = useState<
    string | null
  >(null);
  const [cancelClientBilletTarget, setCancelClientBilletTarget] =
    useState<BilletResponse | null>(null);

  // Aba "NF sem Boleto"
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
    const succeeded: number[] = [];
    const failed: number[] = [];
    for (const id of ids) {
      try {
        await markBilletAsPaid(id);
        succeeded.push(id);
      } catch (error) {
        failed.push(id);
        console.error(error);
      }
    }
    if (succeeded.length > 0) {
      removeBillets(succeeded);
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
          ? `Não foi possível confirmar o pagamento de ${failed.length} boleto(s).`
          : "Não foi possível confirmar o pagamento do boleto",
      );
    } else {
      showError(
        `${succeeded.length} pagamento(s) confirmado(s), ${failed.length} falharam.`,
      );
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
    const succeeded: number[] = [];
    const failed: number[] = [];
    for (const id of ids) {
      try {
        await cancelBillet(id);
        succeeded.push(id);
      } catch (error) {
        failed.push(id);
        console.error(error);
      }
    }
    if (succeeded.length > 0) {
      removeBillets(succeeded);
    }
    if (failed.length === 0) {
      showSuccess(
        succeeded.length > 1
          ? `${succeeded.length} boletos com baixa realizada com sucesso.`
          : "Boleto com baixa realizada com sucesso.",
      );
    } else if (succeeded.length === 0) {
      showError(
        failed.length > 1
          ? `Não foi possível dar baixa em ${failed.length} boletos.`
          : "Não foi possível dar baixa no boleto.",
      );
    } else {
      showError(
        `${succeeded.length} boleto(s) com baixa realizada, ${failed.length} falharam.`,
      );
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

  const handleSearchClientBillets = async (
    client: ClientSelectionInfo | null,
  ) => {
    if (!client) return;
    setLoadingClientBillets(true);
    try {
      const data = await getClientBillets(client.clientId, {
        codigoSituacao: situacao ? Number(situacao) : undefined,
        dataInicio: dataInicio || undefined,
        dataFim: dataFim || undefined,
      });
      setClientBillets(data);
    } catch (error) {
      showError("Não foi possível buscar os boletos do cliente");
      console.error(error);
    } finally {
      setLoadingClientBillets(false);
    }
  };

  const handleClientSelect = (client: ClientSelectionInfo) => {
    setSelectedClient(client);
    handleSearchClientBillets(client);
  };

  const handleApplyFilters = () => {
    handleSearchClientBillets(selectedClient);
  };

  const handleClearFilters = () => {
    setSituacao("");
    setDataInicio("");
    setDataFim("");
    if (selectedClient) {
      setLoadingClientBillets(true);
      getClientBillets(selectedClient.clientId, {})
        .then(setClientBillets)
        .catch((error) => {
          showError("Não foi possível buscar os boletos do cliente");
          console.error(error);
        })
        .finally(() => setLoadingClientBillets(false));
    }
  };

  const handleCancelClientBillet = (billet: BilletResponse) => {
    setCancelClientBilletTarget(billet);
  };

  const confirmCancelClientBillet = async () => {
    if (!cancelClientBilletTarget) return;
    const billet = cancelClientBilletTarget;
    setCancelClientBilletTarget(null);
    setClientBilletActionKey(getClientBilletKey(billet));
    try {
      if (billet.combinedScoreId) {
        await cancelBillet(billet.combinedScoreId);
      } else {
        await cancelBilletByNumber(billet.nossoNumero);
      }
      showSuccess("Boleto com baixa realizada com sucesso.");
      await handleSearchClientBillets(selectedClient);
    } catch (error) {
      showError(
        error instanceof Error
          ? error.message
          : "Não foi possível dar baixa no boleto",
      );
      console.error(error);
    } finally {
      setClientBilletActionKey(null);
    }
  };

  const cancelClientBilletConfirmTitle = cancelClientBilletTarget
    ? `Tem certeza que deseja dar baixa neste boleto (${formatCurrency(
        cancelClientBilletTarget.valor,
      )})? Esta ação não pode ser desfeita.`
    : "";

  const goToGrouping = (clientId: number) => {
    window.open(
      `/comercio/compras?clientId=${clientId}&tab=grouped`,
      "_blank",
      "noopener,noreferrer",
    );
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
    <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col min-h-full">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
            <Receipt className="w-7 h-7 text-primary" />
            Cobranças
          </h1>
          <p className="text-gray-600">
            Acompanhe boletos, notas fiscais pendentes e cancelamentos de
            cobrança dos clientes.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowManualCancelModal(true)}
          className="flex items-center gap-1.5 px-3 py-2 bg-white text-red-700 border border-gray-200 rounded-lg hover:bg-red-50 hover:border-red-200 transition-colors text-sm font-medium cursor-pointer"
        >
          <AlertTriangle className="w-4 h-4" />
          Cancelamento Manual (NF)
        </button>
      </div>

      <ManualCancelModal
        open={showManualCancelModal}
        onClose={() => setShowManualCancelModal(false)}
      />

      <div className="flex gap-2 flex-nowrap overflow-x-auto w-full">
        <button
          type="button"
          className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors cursor-pointer ${
            tab === "abertos"
              ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
          onClick={() => setTab("abertos")}
        >
          <ListChecks className="w-5 h-5 mr-2" />
          Boletos em Aberto
        </button>
        <button
          type="button"
          className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors cursor-pointer ${
            tab === "porCliente"
              ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
          onClick={() => setTab("porCliente")}
        >
          <FileSearch className="w-5 h-5 mr-2" />
          Consultar Boleto
        </button>
        <button
          type="button"
          className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors cursor-pointer ${
            tab === "nfSemBoleto"
              ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
          onClick={() => setTab("nfSemBoleto")}
        >
          <FileText className="w-5 h-5 mr-2" />
          NF sem Boleto
        </button>
      </div>

      <div className="bg-white rounded-b-lg rounded-tr-lg shadow-sm p-6 flex-1">
        {tab === "abertos" && (
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
              <Search
                className="absolute left-3 top-3 text-gray-400"
                size={18}
              />
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
                    {bulkAction === "pay"
                      ? "Confirmando..."
                      : "Marcar como pago"}
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
                        <th className="py-3 px-3 font-semibold">
                          Nº do Boleto
                        </th>
                        <th className="py-3 px-3 font-semibold">Valor</th>
                        <th className="py-3 px-3 font-semibold">Vencimento</th>
                        <th className="py-3 px-3 font-semibold">Situação</th>
                        <th className="py-3 px-3 font-semibold text-right">
                          Ação
                        </th>
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
                          <td className="py-3 px-3">
                            {formatDate(billet.dueDate)}
                          </td>
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
                          onChange={() =>
                            toggleRowSelected(billet.combinedScoreId)
                          }
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
                          <span className="block text-xs text-gray-500">
                            Valor
                          </span>
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
          </div>
        )}

        {tab === "porCliente" && (
          <div>
            <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,20rem)_1fr] gap-6">
              <div className="bg-gray-50 border border-gray-200 rounded-lg">
                <ClientSelector onClientSelect={handleClientSelect} />
              </div>

              <div>
                <div className="flex items-center gap-2 mb-3 text-gray-700 font-medium">
                  <Filter className="w-4 h-4" />
                  Filtros
                </div>
                <div className="flex flex-wrap gap-3 items-end mb-5">
                  <div className="flex flex-col gap-1">
                    <label
                      htmlFor="boletos-situacao"
                      className="text-xs font-medium text-gray-600"
                    >
                      Situação
                    </label>
                    <select
                      id="boletos-situacao"
                      className="border border-gray-300 rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                      value={situacao}
                      onChange={(e) => setSituacao(e.target.value)}
                    >
                      {SITUACAO_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="flex flex-col gap-1">
                    <label
                      htmlFor="boletos-data-inicio"
                      className="text-xs font-medium text-gray-600"
                    >
                      Vencimento de
                    </label>
                    <input
                      id="boletos-data-inicio"
                      type="date"
                      className="border border-gray-300 rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                      value={dataInicio}
                      onChange={(e) => setDataInicio(e.target.value)}
                    />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label
                      htmlFor="boletos-data-fim"
                      className="text-xs font-medium text-gray-600"
                    >
                      até
                    </label>
                    <input
                      id="boletos-data-fim"
                      type="date"
                      className="border border-gray-300 rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                      value={dataFim}
                      onChange={(e) => setDataFim(e.target.value)}
                    />
                  </div>
                  <button
                    type="button"
                    onClick={handleApplyFilters}
                    disabled={!selectedClient}
                    className="flex items-center gap-2 px-4 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                  >
                    <Search className="w-4 h-4" />
                    Buscar
                  </button>
                  <button
                    type="button"
                    onClick={handleClearFilters}
                    disabled={!selectedClient}
                    className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                  >
                    Limpar filtros
                  </button>
                </div>

                {!selectedClient && (
                  <div className="text-center py-16 text-gray-500">
                    <FileSearch className="w-12 h-12 mx-auto mb-3 opacity-40" />
                    <p>
                      Selecione um cliente para consultar os boletos dele no
                      Sicoob
                    </p>
                  </div>
                )}

                {selectedClient && (loadingClientBillets || isLoading) && (
                  <div className="py-16 text-center">
                    <div className="flex justify-center mb-4">
                      <div className="h-20 w-20 rounded-full bg-green-50 flex items-center justify-center">
                        <div className="animate-spin rounded-full h-14 w-14 border-4 border-gray-100 border-t-green-600 border-r-green-600"></div>
                      </div>
                    </div>
                    <p className="text-lg font-medium text-gray-700">
                      Buscando boletos no Sicoob...
                    </p>
                    <p className="text-sm mt-1 text-gray-500">
                      Aguarde enquanto consultamos os boletos do cliente
                    </p>
                  </div>
                )}

                {selectedClient &&
                  !loadingClientBillets &&
                  clientBillets !== null &&
                  clientBillets.length === 0 && (
                    <div className="text-center py-16 text-gray-500">
                      <Receipt className="w-12 h-12 mx-auto mb-3 opacity-40" />
                      <p>
                        Nenhum boleto encontrado para os filtros selecionados
                      </p>
                    </div>
                  )}

                {selectedClient &&
                  !loadingClientBillets &&
                  clientBillets !== null &&
                  clientBillets.length > 0 && (
                    <div className="overflow-x-auto">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="text-left text-gray-500 border-b">
                            <th className="py-3 px-3 font-semibold">Valor</th>
                            <th className="py-3 px-3 font-semibold">
                              Agrupamento
                            </th>
                            <th className="py-3 px-3 font-semibold">Emissão</th>
                            <th className="py-3 px-3 font-semibold">
                              Vencimento
                            </th>
                            <th className="py-3 px-3 font-semibold">
                              Situação
                            </th>
                            <th className="py-3 px-3 font-semibold text-right">
                              Ação
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {clientBillets.map((billet) => (
                            <tr
                              key={`${billet.seuNumero}-${billet.dataVencimento}-${billet.valor}`}
                              className="border-b last:border-0 hover:bg-gray-50 transition-colors"
                            >
                              <td className="py-3 px-3 font-medium text-gray-800">
                                {formatCurrency(billet.valor)}
                              </td>
                              <td className="py-3 px-3 text-gray-500">
                                {billet.combinedScoreId
                                  ? `#${billet.combinedScoreId}`
                                  : "—"}
                              </td>
                              <td className="py-3 px-3">
                                {formatDate(billet.dataEmissao)}
                              </td>
                              <td className="py-3 px-3">
                                {formatDate(billet.dataVencimento)}
                              </td>
                              <td className="py-3 px-3">
                                <span
                                  className={`px-2 py-1 rounded text-xs font-medium ${situacaoBadgeColor(
                                    billet.situacaoBoleto,
                                  )}`}
                                >
                                  {billet.situacaoBoleto}
                                </span>
                              </td>
                              <td className="py-3 px-3 text-right">
                                <div className="flex items-center justify-end gap-2">
                                  {billet.combinedScoreId ? (
                                    <button
                                      type="button"
                                      onClick={() =>
                                        selectedClient &&
                                        goToGrouping(selectedClient.clientId)
                                      }
                                      className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer"
                                    >
                                      <ExternalLink className="w-3 h-3" />
                                      Ver Agrupamento
                                    </button>
                                  ) : (
                                    <span className="text-xs text-gray-400">
                                      Agrupamento não localizado
                                    </span>
                                  )}
                                  {isBilletCancelable(
                                    billet.situacaoBoleto,
                                  ) && (
                                    <button
                                      type="button"
                                      onClick={() =>
                                        handleCancelClientBillet(billet)
                                      }
                                      disabled={
                                        clientBilletActionKey ===
                                        getClientBilletKey(billet)
                                      }
                                      className="inline-flex items-center gap-1 px-3 py-1.5 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                      {clientBilletActionKey ===
                                      getClientBilletKey(billet) ? (
                                        <ActionSpinner />
                                      ) : (
                                        <Trash2 className="w-3 h-3" />
                                      )}
                                      Dar baixa
                                    </button>
                                  )}
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
              </div>
            </div>
          </div>
        )}

        {tab === "nfSemBoleto" && (
          <div>
            <div className="flex flex-wrap justify-between items-center gap-3 mb-5">
              <div>
                <h2 className="text-lg font-semibold text-gray-800">
                  Notas fiscais sem boleto vinculado
                </h2>
                <p className="text-sm text-gray-500">
                  Clientes que só recebem nota fiscal — consideradas vencidas 20
                  dias após a emissão. Confirme o pagamento manualmente quando o
                  cliente quitar.
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
              <Search
                className="absolute left-3 top-3 text-gray-400"
                size={18}
              />
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
                    {invoiceBulkAction === "download"
                      ? "Baixando..."
                      : "Baixar NF"}
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
                        <th className="py-3 px-3 font-semibold text-right">
                          Ação
                        </th>
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
                              checked={isInvoiceRowSelected(
                                invoice.combinedScoreId,
                              )}
                              onChange={() =>
                                toggleInvoiceRowSelected(
                                  invoice.combinedScoreId,
                                )
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
                          <td className="py-3 px-3">
                            {formatDate(invoice.dueDate)}
                          </td>
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
                          checked={isInvoiceRowSelected(
                            invoice.combinedScoreId,
                          )}
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
                            {invoice.invoiceRef
                              ? ` · NF ${invoice.invoiceRef}`
                              : ""}
                          </p>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-2 mt-3 text-sm">
                        <div>
                          <span className="block text-xs text-gray-500">
                            Valor
                          </span>
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
          </div>
        )}
      </div>

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

      <ConfirmDeleteModal
        open={cancelClientBilletTarget !== null}
        onClose={() => setCancelClientBilletTarget(null)}
        onConfirm={confirmCancelClientBillet}
        title={cancelClientBilletConfirmTitle}
      />

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
    </main>
  );
}
