import {
  AlertCircle,
  CalendarClock,
  CheckCircle2,
  Clock,
  Download,
  ExternalLink,
  Trash2,
} from "lucide-react";
import type { BilletResponse, OpenBilletResponse } from "@/types/billetType";
import type { OpenInvoiceResponse } from "@/types/invoiceType";

export type RowActionType = "pay" | "download" | "cancel";
export type BulkActionType = "pay" | "download" | "cancel";

export const SITUACAO_OPTIONS = [
  { value: "", label: "Todas as situações" },
  { value: "1", label: "Em aberto" },
  { value: "2", label: "Baixado" },
  { value: "3", label: "Liquidado" },
];

export function triggerPdfDownload(
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

export function triggerDanfeDownload(
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

export function goToGrouping(clientId: number) {
  window.open(
    `/comercio/compras?clientId=${clientId}&tab=grouped`,
    "_blank",
    "noopener,noreferrer",
  );
}

export function formatCurrency(value: number | null | undefined): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value || 0);
}

export function formatDate(dateString: string | null | undefined): string {
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

export function daysUntil(
  dateString: string | null | undefined,
): number | null {
  if (!dateString) return null;
  const datePart = dateString.split("T")[0];
  const due = new Date(`${datePart}T00:00:00`);
  if (Number.isNaN(due.getTime())) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const diffMs = due.getTime() - today.getTime();
  return Math.round(diffMs / (1000 * 60 * 60 * 24));
}

export function DueBadge({ dueDate }: { dueDate: string | null | undefined }) {
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

export function situacaoBadgeColor(situacao: string): string {
  const value = situacao.toLowerCase();
  if (value.includes("liquidado") || value.includes("pago")) {
    return "bg-green-100 text-green-800";
  }
  if (value.includes("baixado") || value.includes("cancelado")) {
    return "bg-red-100 text-red-800";
  }
  return "bg-blue-100 text-blue-800";
}

export function isBilletCancelable(situacao: string): boolean {
  const value = situacao.toLowerCase();
  return (
    !value.includes("liquidado") &&
    !value.includes("pago") &&
    !value.includes("baixado") &&
    !value.includes("cancelado")
  );
}

export function getClientBilletKey(billet: BilletResponse): string {
  return `${billet.seuNumero}-${billet.dataVencimento}-${billet.valor}`;
}

export function ActionSpinner() {
  return (
    <span className="block w-4 h-4 border-2 border-white/60 border-t-white rounded-full animate-spin" />
  );
}

export function BilletRowActions({
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

export function InvoiceRowActions({
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
