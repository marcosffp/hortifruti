"use client";

import { AlertTriangle, Download, FileText, X } from "lucide-react";
import { useState } from "react";
import { useInvoice } from "@/hooks/useInvoice";
import type { InvoiceResponseGet } from "@/types/invoiceType";
import { showError, showInfo, showSuccess } from "@/utils/toastUtils";
import ConfirmDeleteModal from "./ConfirmDeleteModal";

interface ShowInvoiceDataModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoiceData: InvoiceResponseGet;
  onInvoiceCancelled?: () => void;
}

export default function ShowInvoiceDataModal({
  isOpen,
  onClose,
  invoiceData,
  onInvoiceCancelled,
}: ShowInvoiceDataModalProps) {
  const { getDanfe, getXml, cancelInvoice, isLoading } = useInvoice();
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  const handleDownloadDanfe = async () => {
    try {
      showInfo("Baixando DANFE...");
      const blob = await getDanfe(invoiceData.reference);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      // Use o number da nota fiscal ao invés do reference
      link.setAttribute("download", `NF-${invoiceData.number}.pdf`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      showSuccess("DANFE baixado com sucesso!");
    } catch (error) {
      showError("Erro ao baixar DANFE");
      console.error(error);
    }
  };

  const handleDownloadXml = async () => {
    try {
      showInfo("Baixando XML...");
      const blob = await getXml(invoiceData.reference);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `NFe-${invoiceData.reference}.xml`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      showSuccess("XML baixado com sucesso!");
    } catch (error) {
      showError("Erro ao baixar XML");
      console.error(error);
    }
  };

  const handleCancelInvoice = async () => {
    setShowCancelModal(false);
    setCancelling(true);
    try {
      await cancelInvoice(invoiceData.reference);
      showSuccess("Nota fiscal cancelada com sucesso!");
      onInvoiceCancelled?.();
      onClose();
    } catch (error) {
      showError("Erro ao cancelar nota fiscal");
      console.error(error);
    } finally {
      setCancelling(false);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return "Não definida";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("pt-BR");
    } catch {
      return dateString;
    }
  };

  const getStatusColor = (status: string) => {
    const statusLower = status.toLowerCase();
    if (statusLower.includes("autorizado") || statusLower.includes("emitido")) {
      return "bg-green-100 text-green-800";
    } else if (statusLower.includes("cancelado")) {
      return "bg-red-100 text-red-800";
    } else if (
      statusLower.includes("processando") ||
      statusLower.includes("pendente")
    ) {
      return "bg-yellow-100 text-yellow-800";
    } else {
      return "bg-blue-100 text-blue-800";
    }
  };

  const canCancelInvoice = () => {
    const statusLower = invoiceData.status.toLowerCase();
    return (
      statusLower.includes("autorizado") || statusLower.includes("emitido")
    );
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-auto">
        <div className="sticky top-0 bg-white border-b border-gray-300 p-6 flex justify-between items-center">
          <h2 className="text-xl font-semibold">Informações da Nota Fiscal</h2>
          <button
            type="button"
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6">
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Status:</span>
              <span
                className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(invoiceData.status)}`}
              >
                {invoiceData.status}
              </span>
            </div>

            <div className="py-3 border-t border-gray-200">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Cliente</span>
                <p className="font-semibold text-right">{invoiceData.name}</p>
              </div>
            </div>

            <div className="flex items-center gap-3 py-3 border-t border-gray-200">
              <FileText className="w-5 h-5 text-gray-600" />
              <div className="flex-1">
                <span className="text-sm text-gray-600">Número</span>
                <p className="font-semibold">{invoiceData.number}</p>
              </div>
            </div>

            <div className="flex items-center gap-3 py-3 border-t border-gray-200">
              <span className="text-sm text-gray-600">Referência</span>
              <div className="flex-1 text-right">
                <p className="font-mono text-sm">{invoiceData.reference}</p>
              </div>
            </div>

            <div className="flex items-center gap-3 py-3 border-t border-gray-200">
              <div className="flex-1">
                <span className="text-sm text-gray-600">Valor Total</span>
                <p className="font-semibold text-lg text-green-600">
                  {formatCurrency(invoiceData.totalValue)}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3 py-3 border-t border-gray-200">
              <div>
                <span className="text-sm text-gray-600">Data de Emissão</span>
                <p className="font-semibold">{formatDate(invoiceData.date)}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="sticky bottom-0 bg-white border-t border-gray-300 p-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={handleDownloadDanfe}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2 bg-blue-800 text-white rounded-lg hover:bg-blue-900 transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            <FileText className="w-4 h-4" />
            {isLoading ? "Baixando..." : "Baixar DANFE (PDF)"}
          </button>
          <button
            type="button"
            onClick={handleDownloadXml}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            <Download className="w-4 h-4" />
            Baixar XML
          </button>
          {canCancelInvoice() && (
            <button
              type="button"
              onClick={() => setShowCancelModal(true)}
              disabled={cancelling}
              className="flex items-center gap-2 px-4 py-2 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <AlertTriangle className="w-4 h-4" />
              {cancelling ? "Cancelando..." : "Cancelar Nota Fiscal"}
            </button>
          )}
        </div>
      </div>

      <ConfirmDeleteModal
        open={showCancelModal}
        onClose={() => setShowCancelModal(false)}
        onConfirm={handleCancelInvoice}
        title="Tem certeza que deseja cancelar esta nota fiscal? Esta ação não pode ser desfeita."
      />
    </div>
  );
}
