"use client";

import { Code, Download, FileText, Printer, Receipt, X } from "lucide-react";
import { useEffect, useState } from "react";
import { showError, showSuccess } from "@/services/notificationService";

interface ShowInvoiceAndBilletModalProps {
  isOpen: boolean;
  onClose: () => void;
  danfeBlob: Blob;
  xmlBlob: Blob;
  billetBlob: Blob;
  scoreNumber?: string | number | null;
  invoiceNumber?: string;
  billetNumber?: string;
}

type Tab = "danfe" | "billet";

export default function ShowInvoiceAndBilletModal({
  isOpen,
  onClose,
  danfeBlob,
  xmlBlob,
  billetBlob,
  scoreNumber,
  invoiceNumber,
  billetNumber,
}: ShowInvoiceAndBilletModalProps) {
  const [tab, setTab] = useState<Tab>("danfe");
  const [danfeUrl, setDanfeUrl] = useState<string>("");
  const [billetUrl, setBilletUrl] = useState<string>("");

  useEffect(() => {
    if (!isOpen) return;

    const urls = [danfeBlob, billetBlob].map((blob) =>
      URL.createObjectURL(blob),
    );
    setDanfeUrl(urls[0]);
    setBilletUrl(urls[1]);

    return () => {
      urls.forEach((url) => {
        URL.revokeObjectURL(url);
      });
    };
  }, [danfeBlob, billetBlob, isOpen]);

  if (!isOpen) return null;

  const downloadBlob = (
    blob: Blob,
    fileName: string,
    successMessage: string,
  ) => {
    try {
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", fileName);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      showSuccess(successMessage);
    } catch (error) {
      showError(`Erro ao baixar ${fileName}`);
      console.error(error);
    }
  };

  const handlePrint = () => {
    const url = tab === "danfe" ? danfeUrl : billetUrl;
    try {
      const printWindow = window.open(url, "_blank");
      if (printWindow) {
        printWindow.onload = () => {
          printWindow.print();
        };
      }
    } catch (error) {
      showError("Erro ao imprimir o documento");
      console.error(error);
    }
  };

  const currentUrl = tab === "danfe" ? danfeUrl : billetUrl;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-5xl max-h-[90vh] overflow-hidden flex flex-col">
        <div className="flex justify-between items-center p-6 border-b border-gray-300">
          <div>
            <h2 className="text-xl font-semibold">
              NF + Boleto - Agrupamento {scoreNumber || ""}
            </h2>
            <p className="text-sm text-gray-500 mt-1">
              {invoiceNumber && <>NF nº {invoiceNumber}</>}
              {invoiceNumber && billetNumber && " · "}
              {billetNumber && <>Boleto {billetNumber}</>}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex gap-2 px-6 pt-4">
          <button
            type="button"
            onClick={() => setTab("danfe")}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors cursor-pointer ${
              tab === "danfe"
                ? "bg-blue-800 text-white"
                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
            }`}
          >
            <FileText className="w-4 h-4" />
            DANFE (NF)
          </button>
          <button
            type="button"
            onClick={() => setTab("billet")}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors cursor-pointer ${
              tab === "billet"
                ? "bg-blue-800 text-white"
                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
            }`}
          >
            <Receipt className="w-4 h-4" />
            Boleto
          </button>
        </div>

        <div className="flex-1 overflow-auto p-6">
          {currentUrl ? (
            <iframe
              src={currentUrl}
              className="w-full h-full min-h-[600px] border border-gray-300 rounded"
              title={tab === "danfe" ? "DANFE PDF" : "Boleto PDF"}
            />
          ) : (
            <div className="flex items-center justify-center h-full">
              <p className="text-gray-500">Carregando documento...</p>
            </div>
          )}
        </div>

        <div className="flex flex-wrap justify-end gap-3 p-6 border-t border-gray-300">
          <button
            type="button"
            onClick={handlePrint}
            className="flex items-center gap-2 px-4 py-2 bg-blue-800 text-white rounded-lg hover:bg-blue-900 transition-colors cursor-pointer"
          >
            <Printer className="w-4 h-4" />
            Imprimir
          </button>
          <button
            type="button"
            onClick={() =>
              downloadBlob(
                danfeBlob,
                `NF-${invoiceNumber || scoreNumber}.pdf`,
                "DANFE baixado com sucesso",
              )
            }
            className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors cursor-pointer"
          >
            <Download className="w-4 h-4" />
            Baixar DANFE
          </button>
          <button
            type="button"
            onClick={() =>
              downloadBlob(
                xmlBlob,
                `NF-${invoiceNumber || scoreNumber}.xml`,
                "XML baixado com sucesso",
              )
            }
            className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors cursor-pointer"
          >
            <Code className="w-4 h-4" />
            Baixar XML
          </button>
          <button
            type="button"
            onClick={() =>
              downloadBlob(
                billetBlob,
                `BOL-${billetNumber || scoreNumber}.pdf`,
                "Boleto baixado com sucesso",
              )
            }
            className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors cursor-pointer"
          >
            <Download className="w-4 h-4" />
            Baixar Boleto
          </button>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors cursor-pointer"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
}
