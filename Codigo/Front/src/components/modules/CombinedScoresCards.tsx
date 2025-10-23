"use client";

import React, { useState, useEffect } from "react";
import { FileText, Eye, Trash2, CheckCircle, XCircle, Calendar, Info } from "lucide-react";
import { combinedScoreService } from "@/services/combinedScoreService";
import { billetService } from "@/services/billetService";
import { CombinedScoreType } from "@/types/combinedScoreType";
import { BilletResponse } from "@/types/billetType";
import GroupedProductsModal from "@/components/modals/GroupedProductsModal";
import ShowBilletModal from "@/components/modals/ShowBilletModal";
import ShowBilletDataModal from "@/components/modals/ShowBilletDataModal";
import { useBillet } from "@/hooks/useBillet";
import ClientNumberModal from "../modals/ClientNumberModal";
import { showError, showInfo, showSuccess } from "@/services/notificationService";

interface CombinedScoresCardsProps {
    clientId?: number;
    refreshKey?: number;
}

interface ScoreWithBilletInfo extends CombinedScoreType {
    billetInfo?: BilletResponse | null;
}

export default function CombinedScoresCards({ clientId, refreshKey }: CombinedScoresCardsProps) {
    const [scores, setScores] = useState<ScoreWithBilletInfo[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [selectedScore, setSelectedScore] = useState<ScoreWithBilletInfo | null>(null);
    const [clientNumber, setClientNumber] = useState<string | null>(null);
    const [showModalProducts, setShowModalProducts] = useState(false);
    const [clientNumberModal, setClientNumberModal] = useState({ state: false, groupId: -1 });
    const [showBilletModal, setShowBilletModal] = useState(false);
    const [showBilletDataModal, setShowBilletDataModal] = useState(false);
    const [billetPdf, setBilletPdf] = useState<Blob | null>(null);

    const { generateBillet, getBilletInfo } = useBillet();

    const fetchScores = async () => {
        if (!clientId) {
            setScores([]);
            return;
        }

        setLoading(true);
        try {
            const data = await combinedScoreService.fetchCombinedScores(clientId, page, 10);

            // Para cada score que tem boleto, busca as informações do boleto
            const scoresWithBilletInfo = await Promise.all(
                data.content.map(async (score) => {
                    if (score.hasBillet) {
                        try {
                            const billetInfo = await getBilletInfo(score.id);

                            // Atualiza o status do score baseado no status do boleto
                            if (billetInfo) {
                                const billetStatus = billetInfo.situacaoBoleto.toLowerCase();
                                if (billetStatus.includes("liquidado") || billetStatus.includes("pago")) {
                                    score.status = "PAID";
                                } else if (billetStatus.includes("cancelado") || billetStatus.includes("baixado")) {
                                    score.status = "CANCELLED";
                                } else if (billetStatus.includes("aberto") || billetStatus.includes("pendente")) {
                                    // Verifica se está vencido
                                    const vencimento = new Date(billetInfo.dataVencimento);
                                    const hoje = new Date();
                                    if (vencimento < hoje) {
                                        score.status = "OVERDUE";
                                    } else {
                                        score.status = "PENDING";
                                    }
                                }
                            }

                            return { ...score, billetInfo };
                        } catch (error) {
                            console.error(`Erro ao buscar info do boleto ${score.id}:`, error);
                            return { ...score, billetInfo: null };
                        }
                    }
                    return { ...score, billetInfo: null };
                })
            );

            setScores(scoresWithBilletInfo);
            setTotalPages(data.totalPages);
        } catch (error) {
            showError("Erro ao carregar agrupamentos");
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchScores();
    }, [clientId, page, refreshKey]);

    const handleDelete = async (id: number, number: string) => {
        if (!confirm(`Tem certeza que deseja deletar o agrupamento ${number || id}?`)) return;

        try {
            await combinedScoreService.cancelGrouping(id);
            showSuccess("Agrupamento deletado com sucesso");
            fetchScores();
        } catch (error) {
            showError("Erro ao deletar agrupamento");
            console.error(error);
        }
    };

    const handleTogglePayment = async (score: ScoreWithBilletInfo) => {
        try {
            if (score.status === "PAID") {
                await combinedScoreService.cancelPayment(score.id);
                showSuccess("Pagamento cancelado com sucesso");
            } else {
                await combinedScoreService.confirmPayment(score.id);
                showSuccess("Pagamento confirmado com sucesso");
            }
            fetchScores();
        } catch (error) {
            showError("Erro ao atualizar pagamento");
            console.error(error);
        }
    };

    const handleViewProducts = (score: ScoreWithBilletInfo) => {
        setSelectedScore(score);
        setShowModalProducts(true);
    };

    const handleGenerateBillet = async (scoreId: number, clientNumber: string) => {
        try {
            const score = scores.find(s => s.id === scoreId);
            if (!score) {
                showError("Agrupamento não encontrado");
                return;
            }

            const pdfBlob = await generateBillet(scoreId, clientNumber);

            setSelectedScore(score);
            setClientNumber(clientNumber);

            setBilletPdf(pdfBlob);
            setShowBilletModal(true);

            showSuccess("Boleto gerado com sucesso!");

            fetchScores();
        } catch (error) {
            showError("Erro ao gerar boleto");
            console.error(error);
        }
    };

    const handleShowBillet = async (score: ScoreWithBilletInfo) => {
        try {
            setSelectedScore(score);

            // Se já tem as informações do boleto, abre o modal de dados
            showInfo("Buscando boleto...");
            if (score.billetInfo) {
                setShowBilletDataModal(true);
            } else {
                setClientNumberModal({ state: true, groupId: score.id }); // Abre modal para inserir número do cliente
            }
        } catch (error) {
            showError("Erro ao buscar boleto");
            console.error(error);
        }
    };

    const formatDate = (dateString: string | null) => {
        if (!dateString) return "Não definida";
        try {
            const datePart = dateString.split('T')[0];
            const [year, month, day] = datePart.split('-');
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
        switch (status) {
            case "PAGO":
                return "bg-green-100 text-green-800";
            case "PENDENTE":
                return "bg-yellow-100 text-yellow-800";
            case "BAIXADO":
            case "CANCELADO":
                return "bg-red-100 text-red-800";
            default:
                return "bg-blue-100 text-blue-800";
        }
    };

    const getStatusLabel = (status: string) => {
        switch (status) {
            case "PAID":
                return "PAGO";
            case "PENDING":
                return "PENDENTE";
            case "OVERDUE":
                return "VENCIDO";
            case "CANCELLED":
                return "CANCELADO";
            default:
                return status;
        }
    };

    // Verifica se o boleto está em aberto (não permite deletar)
    const isBilletOpen = (score: ScoreWithBilletInfo): boolean => {
        if (!score.billetInfo) return false;
        const status = score.billetInfo.situacaoBoleto.toLowerCase();
        return status.includes("aberto") || status.includes("pendente");
    };

    if (!clientId) {
        return (
            <div className="text-center py-12 text-gray-500">
                <p>Selecione um cliente para visualizar os agrupamentos</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Loading state */}
            {loading ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {[...Array(6)].map((_, i) => (
                        <div key={i} className="h-72 bg-gray-200 animate-pulse rounded-lg" />
                    ))}
                </div>
            ) : scores.length === 0 ? (
                <div className="text-center py-12 text-gray-500">
                    <FileText className="w-12 h-12 mx-auto mb-3 opacity-50" />
                    <p>Nenhum agrupamento encontrado</p>
                    <p className="text-sm mt-2">
                        Use o botão "Criar Agrupamento por Período" na aba anterior
                    </p>
                </div>
            ) : (
                <>
                    {/* Cards Grid */}
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {scores.map((score) => (
                            <div
                                key={score.id}
                                className="bg-white border border-gray-200 rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow"
                            >
                                {/* Header do Card */}
                                <div className="flex justify-between items-start mb-4">
                                    <div>
                                        <h3 className="font-semibold text-lg">
                                            Agrupamento {score.number || score.id}
                                        </h3>
                                        <p className="text-sm text-gray-500 flex items-center gap-1 mt-1">
                                            <Calendar className="w-3 h-3" />
                                            {formatDate(score.confirmedAt)}
                                        </p>
                                    </div>
                                    <span
                                        className={`px-2 py-1 rounded text-xs font-medium ${getStatusColor(
                                            score.status
                                        )}`}
                                    >
                                        {getStatusLabel(score.status)}
                                    </span>
                                </div>

                                {/* Informações */}
                                <div className="space-y-2 mb-4 pb-4 border-b">
                                    <div className="flex justify-between text-sm">
                                        <span className="text-gray-600">Valor Total:</span>
                                        <span className="font-semibold">{formatCurrency(score.totalValue)}</span>
                                    </div>
                                    <div className="flex justify-between text-sm">
                                        <span className="text-gray-600">Vencimento:</span>
                                        <span>{formatDate(score.dueDate)}</span>
                                    </div>
                                    <div className="flex gap-2 text-xs mt-2">
                                        {score.hasBillet && (
                                            <span className="px-2 py-1 bg-blue-50 text-blue-700 rounded">
                                                Boleto gerado
                                            </span>
                                        )}
                                        {score.hasInvoice && (
                                            <span className="px-2 py-1 bg-purple-50 text-purple-700 rounded">
                                                Nota gerada
                                            </span>
                                        )}
                                    </div>
                                </div>

                                {/* Ações */}
                                <div className="space-y-2">
                                    {/* Botão Ver Produtos */}
                                    <button
                                        onClick={() => handleViewProducts(score)}
                                        className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm"
                                    >
                                        <Eye className="w-4 h-4" />
                                        Ver Produtos
                                    </button>

                                    {/* Botões de Boleto e Nota Fiscal */}
                                    <div className="grid grid-cols-2 gap-2">
                                        {score.hasBillet ? (
                                            <button
                                                onClick={() => handleShowBillet(score)}
                                                className="flex items-center justify-center gap-1 px-2 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors text-xs"
                                            >
                                                <Info className="w-3 h-3" />
                                                Ver Boleto
                                            </button>
                                        ) : (
                                            <button
                                                onClick={() => setClientNumberModal({ state: true, groupId: score.id })}
                                                className="flex items-center justify-center gap-1 px-2 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-xs"
                                            >
                                                <FileText className="w-3 h-3" />
                                                Gerar Boleto
                                            </button>
                                        )}

                                        {score.hasInvoice ? (
                                            <button
                                                onClick={() => showInfo("Funcionalidade de consultar NF em desenvolvimento")}
                                                className="flex items-center justify-center gap-1 px-2 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors text-xs"
                                            >
                                                <FileText className="w-3 h-3" />
                                                Consultar NF
                                            </button>
                                        ) : (
                                            <button
                                                onClick={() => showInfo("Funcionalidade de gerar NF em desenvolvimento")}
                                                className="flex items-center justify-center gap-1 px-2 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-xs"
                                            >
                                                <FileText className="w-3 h-3" />
                                                Gerar NF
                                            </button>
                                        )}
                                    </div>

                                    {/* Confirmar Pagamento e Deletar */}
                                    <div className="grid grid-cols-2 gap-2">
                                        {!score.hasBillet && (
                                            <button
                                                onClick={() => handleTogglePayment(score)}
                                                className={`flex items-center justify-center gap-1 px-2 py-2 rounded-lg transition-colors text-xs ${score.status === "PAID"
                                                        ? "bg-yellow-600 text-white hover:bg-yellow-700"
                                                        : "bg-green-600 text-white hover:bg-green-700"
                                                    }`}
                                            >
                                                {score.status === "PAID" ? (
                                                    <>
                                                        <XCircle className="w-3 h-3" />
                                                        Cancelar Pag.
                                                    </>
                                                ) : (
                                                    <>
                                                        <CheckCircle className="w-3 h-3" />
                                                        Confirmar Pag.
                                                    </>
                                                )}
                                            </button>
                                        )}
                                        {/* Esconde botão deletar se boleto estiver em aberto */}
                                        {!isBilletOpen(score) && (
                                            <button
                                                onClick={() => handleDelete(score.id, score.number)}
                                                className={`flex items-center justify-center gap-1 px-2 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors text-xs ${!score.hasBillet ? "" : "col-span-2"
                                                    }`}
                                            >
                                                <Trash2 className="w-3 h-3" />
                                                Deletar
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Paginação */}
                    {totalPages > 1 && (
                        <div className="flex justify-center items-center gap-2 mt-6">
                            <button
                                onClick={() => setPage((p) => Math.max(0, p - 1))}
                                disabled={page === 0}
                                className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                            >
                                Anterior
                            </button>
                            <span className="text-sm">
                                Página {page + 1} de {totalPages}
                            </span>
                            <button
                                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                                disabled={page >= totalPages - 1}
                                className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                            >
                                Próxima
                            </button>
                        </div>
                    )}
                </>
            )}

            {/* Modal de produtos */}
            {showModalProducts && selectedScore && (
                <GroupedProductsModal
                    combinedScoreId={selectedScore.id}
                    scoreNumber={selectedScore.number}
                    onClose={() => {
                        setShowModalProducts(false);
                        setSelectedScore(null);
                    }}
                />
            )}

            {/* Modal de boleto */}
            {showBilletModal && billetPdf && selectedScore && (
                <ShowBilletModal
                    isOpen={showBilletModal}
                    onClose={() => {
                        setShowBilletModal(false);
                        setBilletPdf(null);
                        setSelectedScore(null);
                        setClientNumber(null);
                    }}
                    billetData={billetPdf}
                    scoreNumber={selectedScore.number || selectedScore.id}
                    clientNumber={clientNumber}
                />
            )}

            {showBilletDataModal && selectedScore?.billetInfo && (
                <ShowBilletDataModal
                    isOpen={showBilletDataModal}
                    onClose={() => {
                        setShowBilletDataModal(false);
                        setSelectedScore(null);
                    }}
                    billetData={selectedScore.billetInfo}
                    combinedScoreId={selectedScore.id}
                    clientNumber={selectedScore.number || selectedScore.billetInfo?.seuNumero || null}
                    onBilletCancelled={() => {
                        fetchScores(); // Recarrega os dados após cancelamento
                    }}
                />
            )}

            <ClientNumberModal
                open={clientNumberModal.state}
                onClose={() => setClientNumberModal({ state: false, groupId: -1 })}
                onConfirm={(number) => {
                    setClientNumberModal({ state: false, groupId: -1 });
                    handleGenerateBillet(clientNumberModal.groupId, number);
                }}
            />
        </div>
    );
}
