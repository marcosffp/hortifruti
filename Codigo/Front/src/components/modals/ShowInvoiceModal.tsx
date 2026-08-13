"use client";

import { Download, Printer, X } from "lucide-react";
import { useEffect, useState } from "react";
import { showError, showSuccess } from "@/utils/toastUtils";

interface ShowInvoiceModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoiceData: Blob;
  scoreNumber?: string | number | null;
  ref: string;
  invoiceNumber?: string;
}

export default function ShowInvoiceModal({
  isOpen,
  onClose,
  invoiceData,
  scoreNumber,
  ref,
  invoiceNumber,
}: ShowInvoiceModalProps) {
  const [pdfUrl, setPdfUrl] = useState<string>("");

  useEffect(() => {
    if (invoiceData && isOpen) {
      const url = URL.createObjectURL(invoiceData);
      setPdfUrl(url);

      return () => {
        URL.revokeObjectURL(url);
      };
    }
  }, [invoiceData, isOpen]);

  const handleDownload = () => {
    try {
      const url = URL.createObjectURL(invoiceData);
      const link = document.createElement("a");
      link.href = url;
      const fileName = invoiceNumber
        ? `NF-${invoiceNumber}.pdf`
        : `NF-${scoreNumber || ref}.pdf`;

      link.setAttribute("download", fileName);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      showSuccess("DANFE baixado com sucesso");
    } catch (error) {
      showError("Erro ao baixar o DANFE");
      console.error(error);
    }
  };

  const handlePrint = () => {
    try {
      const printWindow = window.open(pdfUrl, "_blank");
      if (printWindow) {
        printWindow.onload = () => {
          printWindow.print();
        };
      }
    } catch (error) {
      showError("Erro ao imprimir o DANFE");
      console.error(error);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-5xl max-h-[90vh] overflow-hidden flex flex-col">
        <div className="flex justify-between items-center p-6 border-b border-gray-300">
          <h2 className="text-xl font-semibold">
            Nota Fiscal - Agrupamento {scoreNumber || ""}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 overflow-auto p-6">
          {pdfUrl ? (
            <iframe
              src={pdfUrl}
              className="w-full h-full min-h-[600px] border border-gray-300 rounded"
              title="DANFE PDF"
            />
          ) : (
            <div className="flex items-center justify-center h-full">
              <p className="text-gray-500">Carregando nota fiscal...</p>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 p-6 border-t border-gray-300">
          <button
            type="button"
            onClick={handlePrint}
            className="flex items-center gap-2 px-4 py-2 bg-blue-800 text-white rounded-lg hover:bg-blue-900 transition-colors"
          >
            <Printer className="w-4 h-4" />
            Imprimir
          </button>
          <button
            type="button"
            onClick={handleDownload}
            className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
          >
            <Download className="w-4 h-4" />
            Baixar PDF
          </button>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
}
