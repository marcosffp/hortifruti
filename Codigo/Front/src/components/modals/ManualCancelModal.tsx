"use client";

import { AlertTriangle, FileX, X } from "lucide-react";
import { useState } from "react";
import { useInvoice } from "@/hooks/useInvoice";
import { showError, showSuccess } from "@/services/notificationService";
import ConfirmDeleteModal from "./ConfirmDeleteModal";

interface ManualCancelModalProps {
  open: boolean;
  onClose: () => void;
}

export default function ManualCancelModal({
  open,
  onClose,
}: ManualCancelModalProps) {
  const { cancelInvoice } = useInvoice();

  const [invoiceRef, setInvoiceRef] = useState("");
  const [cancellingInvoice, setCancellingInvoice] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  if (!open) return null;

  const handleClose = () => {
    setInvoiceRef("");
    onClose();
  };

  const handleCancelInvoice = async () => {
    setShowConfirmModal(false);
    const ref = invoiceRef.trim();
    setCancellingInvoice(true);
    try {
      await cancelInvoice(ref);
      showSuccess("Nota fiscal cancelada com sucesso!");
      setInvoiceRef("");
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Erro ao cancelar nota fiscal",
      );
      console.error(error);
    } finally {
      setCancellingInvoice(false);
    }
  };

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
        <div className="bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[90vh] overflow-auto">
          <div className="sticky top-0 bg-white border-b border-gray-300 p-6 flex justify-between items-center">
            <div>
              <h2 className="text-xl font-semibold">
                Cancelamento Manual de Nota Fiscal
              </h2>
              <p className="text-sm text-gray-500 mt-1">
                Cancelamento extemporâneo de nota fiscal por número/ref, sem
                precisar localizar o agrupamento.
              </p>
            </div>
            <button
              type="button"
              onClick={handleClose}
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="p-6">
            <div className="border border-gray-200 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-3">
                <FileX className="w-5 h-5 text-gray-600" />
                <h3 className="font-semibold">Cancelar Nota Fiscal</h3>
              </div>

              <label
                htmlFor="manual-cancel-invoice-ref"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Referência (ref) da Nota Fiscal *
              </label>
              <input
                id="manual-cancel-invoice-ref"
                type="text"
                value={invoiceRef}
                onChange={(e) => setInvoiceRef(e.target.value)}
                placeholder="Digite o id/ref da nota fiscal"
                className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-red-500 mb-4"
              />

              <div className="flex justify-end">
                <button
                  type="button"
                  onClick={() => setShowConfirmModal(true)}
                  disabled={cancellingInvoice || !invoiceRef.trim()}
                  className="flex items-center gap-2 px-4 py-2 bg-red-600/80 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <AlertTriangle className="w-4 h-4" />
                  {cancellingInvoice ? "Cancelando..." : "Cancelar Nota Fiscal"}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <ConfirmDeleteModal
        open={showConfirmModal}
        onClose={() => setShowConfirmModal(false)}
        onConfirm={handleCancelInvoice}
        title={`Tem certeza que deseja cancelar a nota fiscal de referência "${invoiceRef.trim()}" (cancelamento extemporâneo)? Esta ação é enviada diretamente à Focus NFe e não pode ser desfeita.`}
      />
    </>
  );
}
