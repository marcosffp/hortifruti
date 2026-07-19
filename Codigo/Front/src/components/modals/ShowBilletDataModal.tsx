"use client";

import {
  Calendar,
  DollarSign,
  Download,
  FileText,
  Trash2,
  User,
  X,
} from "lucide-react";
import { useState } from "react";
import { useBillet } from "@/hooks/useBillet";
import {
  showError,
  showInfo,
  showSuccess,
} from "@/services/notificationService";
import type { BilletResponse } from "@/types/billetType";
import ConfirmDeleteModal from "./ConfirmDeleteModal";

interface ShowBilletDataModalProps {
  isOpen: boolean;
  onClose: () => void;
  billetData: BilletResponse;
  combinedScoreId: number;
  clientNumber: string | null;
  onBilletCancelled?: () => void;
}

export default function ShowBilletDataModal({
  isOpen,
  onClose,
  billetData,
  combinedScoreId,
  clientNumber,
  onBilletCancelled,
}: ShowBilletDataModalProps) {
  const { issueCopy, downloadStoredBillet, cancelBillet, isLoading } =
    useBillet();
  const [cancelling, setCancelling] = useState(false);
  const [downloadingStored, setDownloadingStored] = useState(false);
  const [confirmDeleteModalOpen, setConfirmDeleteModalOpen] = useState(false);

  if (!isOpen) return null;

  const handleIssueCopy = async () => {
    try {
      showInfo("Gerando segunda via...");
      const pdfBlob = await issueCopy(combinedScoreId);

      const url = window.URL.createObjectURL(pdfBlob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `BOL-${clientNumber}_SEGUNDA_VIA.pdf`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      showSuccess("Segunda via gerada com sucesso!");
    } catch (error) {
      showError("Erro ao gerar segunda via do boleto");
      console.error(error);
    }
  };

  const handleDownloadStoredBillet = async () => {
    setDownloadingStored(true);
    try {
      const pdfBlob = await downloadStoredBillet(combinedScoreId);

      const url = window.URL.createObjectURL(pdfBlob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `BOL-${clientNumber}.pdf`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      showSuccess("Boleto baixado com sucesso!");
    } catch (error) {
      showError("Erro ao baixar boleto armazenado");
      console.error(error);
    } finally {
      setDownloadingStored(false);
    }
  };

  const handleCancelBillet = async () => {
    setConfirmDeleteModalOpen(false);
    setCancelling(true);
    try {
      await cancelBillet(combinedScoreId);
      showSuccess("Boleto cancelado com sucesso!");
      onBilletCancelled?.();
      onClose();
    } catch (error) {
      showError("Erro ao cancelar boleto");
      console.error(error);
    } finally {
      setCancelling(false);
    }
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return "Não definida";
    try {
      const [year, month, day] = dateString.split("-");
      return `${day}/${month}/${year}`;
    } catch {
      return dateString;
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);
  };

  const getStatusColor = (status: string) => {
    const statusLower = status.toLowerCase();
    if (statusLower.includes("liquidado") || statusLower.includes("pago")) {
      return "bg-green-100 text-green-800";
    }
    if (statusLower.includes("aberto") || statusLower.includes("pendente")) {
      return "bg-yellow-100 text-yellow-800";
    }
    if (statusLower.includes("vencido")) {
      return "bg-red-100 text-red-800";
    }
    if (statusLower.includes("cancelado") || statusLower.includes("baixado")) {
      return "bg-gray-100 text-gray-800";
    }
    return "bg-blue-100 text-blue-800";
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-auto">
        <div className="sticky top-0 bg-white border-b border-gray-300 p-6 flex justify-between items-center">
          <h2 className="text-xl font-semibold">
            BOL-{clientNumber || "Agrupamento"}
          </h2>
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
              <span className="text-sm text-gray-600">Status do Boleto</span>
              <span
                className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(billetData.situacaoBoleto)}`}
              >
                {billetData.situacaoBoleto}
              </span>
            </div>

            <div className="flex items-center gap-3 py-3 border-t border-gray-200">
              <User className="w-5 h-5 text-gray-600" />
              <div className="flex-1">
                <span className="text-sm text-gray-600">Pagador</span>
                <p className="font-semibold">{billetData.nomePagador}</p>
              </div>
            </div>

            <div className="flex items-center gap-3 py-3 border-t border-gray-200">
              <FileText className="w-5 h-5 text-gray-600" />
              <div className="flex-1">
                <span className="text-sm text-gray-600">Seu Número</span>
                <p className="font-semibold">{billetData.seuNumero}</p>
              </div>
            </div>

            <div className="flex items-center gap-3 py-3 border-t border-gray-200">
              <DollarSign className="w-5 h-5 text-gray-600" />
              <div className="flex-1">
                <span className="text-sm text-gray-600">Valor</span>
                <p className="font-semibold text-lg text-green-600">
                  {formatCurrency(Number(billetData.valor))}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4 py-3 border-t border-gray-200">
              <div className="flex items-center gap-3">
                <Calendar className="w-5 h-5 text-gray-600" />
                <div>
                  <span className="text-sm text-gray-600 block">
                    Data de Emissão
                  </span>
                  <p className="font-semibold">
                    {formatDate(billetData.dataEmissao)}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Calendar className="w-5 h-5 text-gray-600" />
                <div>
                  <span className="text-sm text-gray-600 block">
                    Vencimento
                  </span>
                  <p className="font-semibold">
                    {formatDate(billetData.dataVencimento)}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="sticky bottom-0 bg-white border-t border-gray-300 p-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={handleDownloadStoredBillet}
            disabled={downloadingStored}
            className="flex items-center gap-2 px-4 py-2 bg-green-700 text-white rounded-lg hover:bg-green-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Download className="w-4 h-4" />
            {downloadingStored ? "Baixando..." : "Baixar Boleto"}
          </button>
          <button
            type="button"
            onClick={handleIssueCopy}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2 bg-blue-800 text-white rounded-lg hover:bg-blue-900 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Download className="w-4 h-4" />
            {isLoading ? "Gerando..." : "Segunda Via"}
          </button>
          <button
            type="button"
            onClick={() => setConfirmDeleteModalOpen(true)}
            disabled={cancelling || isLoading}
            className="flex items-center gap-2 px-4 py-2 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Trash2 className="w-4 h-4" />
            {cancelling ? "Cancelando..." : "Dar Baixa"}
          </button>
        </div>
      </div>

      <ConfirmDeleteModal
        open={confirmDeleteModalOpen}
        onClose={() => setConfirmDeleteModalOpen(false)}
        onConfirm={handleCancelBillet}
        title="Tem certeza que deseja dar baixa neste boleto? Esta ação não pode ser desfeita."
      />
    </div>
  );
}
