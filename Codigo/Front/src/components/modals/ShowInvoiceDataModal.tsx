"use client";

import { useInvoice } from "@/hooks/useInvoice";
import { showSuccess, showError, showInfo } from "@/services/notificationService";
import { InvoiceResponseGet } from "@/types/invoiceType";
import { X, Download, FileText, AlertTriangle } from "lucide-react";
import { useState } from "react";

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
    onInvoiceCancelled 
}: ShowInvoiceDataModalProps) {
    const { getDanfe, getXml, cancelInvoice, isLoading } = useInvoice();
    const [showCancelModal, setShowCancelModal] = useState(false);
    const [justificativa, setJustificativa] = useState("");
    const [cancelling, setCancelling] = useState(false);

    const handleDownloadDanfe = async () => {
        try {
            showInfo("Baixando DANFE...");
            const blob = await getDanfe(invoiceData.reference);
            
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `DANFE-${invoiceData.reference}.pdf`);
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
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `NFe-${invoiceData.reference}.xml`);
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
        if (!justificativa.trim() || justificativa.trim().length < 15) {
            showError("A justificativa deve ter no mínimo 15 caracteres");
            return;
        }

        if (!confirm("Tem certeza que deseja cancelar esta nota fiscal? Esta ação não pode ser desfeita.")) {
            return;
        }

        setCancelling(true);
        try {
            await cancelInvoice(invoiceData.reference, justificativa);
            showSuccess("Nota fiscal cancelada com sucesso!");
            setShowCancelModal(false);
            setJustificativa("");
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
        } else if (statusLower.includes("processando") || statusLower.includes("pendente")) {
            return "bg-yellow-100 text-yellow-800";
        } else {
            return "bg-blue-100 text-blue-800";
        }
    };

    const canCancelInvoice = () => {
        const statusLower = invoiceData.status.toLowerCase();
        return statusLower.includes("autorizado") || statusLower.includes("emitido");
    };

    if (!isOpen) return null;

    return (
        <>
            <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
                    {/* Header */}
                    <div className="flex justify-between items-center p-6 border-b border-gray-300">
                        <h2 className="text-xl font-semibold">Informações da Nota Fiscal</h2>
                        <button
                            onClick={onClose}
                            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                        >
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    {/* Conteúdo */}
                    <div className="flex-1 overflow-auto p-6">
                        <div className="space-y-4">
                            {/* Status */}
                            <div className="flex justify-between items-center pb-4 border-b">
                                <span className="text-gray-600 font-medium">Status:</span>
                                <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(invoiceData.status)}`}>
                                    {invoiceData.status}
                                </span>
                            </div>

                            {/* Informações */}
                            <div className="space-y-3">
                                <div className="flex justify-between">
                                    <span className="text-gray-600">Cliente:</span>
                                    <span className="font-semibold text-right">{invoiceData.name}</span>
                                </div>
                                <div className="flex justify-between">
                                    <span className="text-gray-600">Número:</span>
                                    <span className="font-semibold">{invoiceData.number}</span>
                                </div>
                                <div className="flex justify-between">
                                    <span className="text-gray-600">Referência:</span>
                                    <span className="font-mono text-sm">{invoiceData.reference}</span>
                                </div>
                                <div className="flex justify-between">
                                    <span className="text-gray-600">Valor Total:</span>
                                    <span className="font-semibold text-green-600">
                                        {formatCurrency(invoiceData.totalValue)}
                                    </span>
                                </div>
                                <div className="flex justify-between">
                                    <span className="text-gray-600">Data de Emissão:</span>
                                    <span>{formatDate(invoiceData.date)}</span>
                                </div>
                            </div>

                            {/* Ações de Download */}
                            <div className="pt-4 border-t space-y-3">
                                <h3 className="font-semibold text-gray-700 mb-2">Downloads</h3>
                                <button
                                    onClick={handleDownloadDanfe}
                                    disabled={isLoading}
                                    className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    <FileText className="w-5 h-5" />
                                    Baixar DANFE (PDF)
                                </button>
                                <button
                                    onClick={handleDownloadXml}
                                    disabled={isLoading}
                                    className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    <Download className="w-5 h-5" />
                                    Baixar XML
                                </button>
                            </div>

                            {/* Cancelamento */}
                            {canCancelInvoice() && (
                                <div className="pt-4 border-t">
                                    <button
                                        onClick={() => setShowCancelModal(true)}
                                        className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                                    >
                                        <AlertTriangle className="w-5 h-5" />
                                        Cancelar Nota Fiscal
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Footer */}
                    <div className="flex justify-end gap-3 p-6 border-t border-gray-300">
                        <button
                            onClick={onClose}
                            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
                        >
                            Fechar
                        </button>
                    </div>
                </div>
            </div>

            {/* Modal de Cancelamento */}
            {showCancelModal && (
                <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-[60] p-4">
                    <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
                        <div className="p-6">
                            <div className="flex items-center gap-3 mb-4">
                                <div className="p-3 bg-red-100 rounded-full">
                                    <AlertTriangle className="w-6 h-6 text-red-600" />
                                </div>
                                <h3 className="text-lg font-semibold">Cancelar Nota Fiscal</h3>
                            </div>
                            
                            <p className="text-gray-600 mb-4">
                                Para cancelar a nota fiscal, é necessário informar uma justificativa com no mínimo 15 caracteres.
                            </p>

                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Justificativa *
                                </label>
                                <textarea
                                    value={justificativa}
                                    onChange={(e) => setJustificativa(e.target.value)}
                                    placeholder="Digite a justificativa para o cancelamento..."
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 min-h-[100px]"
                                    maxLength={255}
                                />
                                <p className="text-xs text-gray-500 mt-1">
                                    {justificativa.length}/255 caracteres (mínimo 15)
                                </p>
                            </div>

                            <div className="flex gap-3">
                                <button
                                    onClick={() => {
                                        setShowCancelModal(false);
                                        setJustificativa("");
                                    }}
                                    disabled={cancelling}
                                    className="flex-1 px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors disabled:opacity-50"
                                >
                                    Voltar
                                </button>
                                <button
                                    onClick={handleCancelInvoice}
                                    disabled={cancelling || justificativa.trim().length < 15}
                                    className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {cancelling ? "Cancelando..." : "Confirmar Cancelamento"}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
