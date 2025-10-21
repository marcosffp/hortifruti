"use client";

import React, { useState, useEffect, useRef } from "react";
import { Trash, Eye } from "lucide-react";
import { purchaseService } from "@/services/purchaseService";
import { PurchaseType } from "@/types/purchaseType";
import InvoiceProductsModal from "@/components/modals/InvoiceProductsModal";
import { combinedScoreService } from "@/services/combinedScoreService";
import { showError, showSuccess } from "@/services/notificationService";

interface PurchaseFilesTableProps {
    clientId?: number;
    refreshKey?: number;
    startDate?: string;
    endDate?: string;
}

export default function PurchaseFilesTable({ clientId, refreshKey }: PurchaseFilesTableProps) {
    const [purchases, setPurchases] = useState<PurchaseType[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [selectedPurchase, setSelectedPurchase] = useState<PurchaseType | null>(null);
    const [showModal, setShowModal] = useState(false);
    const [groupBy, setGroupBy] = useState<'week' | 'month' | 'custom'>('custom');
    const [creatingGrouping, setCreatingGrouping] = useState(false);
    const [startDate, setStartDate] = useState(() => {
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
        return firstDay.toISOString().split('T')[0];
    });
    const [endDate, setEndDate] = useState(() => {
        const now = new Date();
        const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
        return lastDay.toISOString().split('T')[0];
    });

    // Debounce refs
    const debounceTimer = useRef<NodeJS.Timeout | null>(null);

    function getWeekInterval() {
        const today = new Date();
        let lastMonday = new Date(today);
        lastMonday.setDate(today.getDate() - ((today.getDay() + 6) % 7) - 7);
        lastMonday.setHours(0, 0, 0, 0);

        let lastSaturday = new Date(lastMonday);
        lastSaturday.setDate(lastMonday.getDate() + 6);
        lastSaturday.setHours(23, 59, 59, 999);

        return {
            start: lastMonday.toISOString().split('T')[0],
            end: lastSaturday.toISOString().split('T')[0],
        };
    }

    function getLastMonthInterval() {
        const today = new Date();
        const year = today.getMonth() === 0 ? today.getFullYear() - 1 : today.getFullYear();
        const month = today.getMonth() === 0 ? 11 : today.getMonth() - 1;

        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);

        return {
            start: firstDay.toISOString().split('T')[0],
            end: lastDay.toISOString().split('T')[0],
        };
    }

    useEffect(() => {
        if (groupBy === 'week') {
            const { start, end } = getWeekInterval();
            setStartDate(start);
            setEndDate(end);
        } else if (groupBy === 'month') {
            const { start, end } = getLastMonthInterval();
            setStartDate(start);
            setEndDate(end);
        }
        // Se for custom, não altera nada
    }, [groupBy]);

    const fetchPurchases = async () => {
        if (!clientId) {
            setPurchases([]);
            return;
        }

        setLoading(true);
        try {
            const data = await purchaseService.fetchPurchaseFiles(clientId, page, 10);
            setPurchases(data.content || []);
            setTotalPages(data.totalPages || 0);
        } catch (error) {
            showError("Erro ao carregar arquivos de compra");
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (debounceTimer.current) clearTimeout(debounceTimer.current);

        debounceTimer.current = setTimeout(() => {
            fetchPurchases();
        }, 300);

        return () => {
            if (debounceTimer.current) clearTimeout(debounceTimer.current);
        };
    }, [clientId, page, refreshKey, startDate, endDate]);

    const handleDelete = async (purchaseId: number) => {
        if (!confirm("Tem certeza que deseja deletar este arquivo de compra?")) return;

        try {
            await purchaseService.deletePurchaseFile(purchaseId);
            showSuccess("Arquivo deletado com sucesso");
            fetchPurchases();
        } catch (error) {
            showError("Erro ao deletar arquivo");
            console.error(error);
        }
    };

    const handleViewProducts = (purchase: PurchaseType) => {
        setSelectedPurchase(purchase);
        setShowModal(true);
    };
    
    const handleConfirmGrouping = async () => {
        if (!clientId) {
            showError("Selecione um cliente primeiro");
            return;
        }

        if (!startDate || !endDate) {
            showError("Selecione o período (data início e fim)");
            return;
        }

        if (new Date(startDate) > new Date(endDate)) {
            showError("A data inicial não pode ser posterior à data final");
            return;
        }

        setCreatingGrouping(true);
        try {
            const response = await combinedScoreService.createCombinedScore({
                clientId,
                startDate,
                endDate,
            });
            showSuccess("Agrupamento criado com sucesso! Veja na aba 'Produtos Agrupados'");
        } catch (error: any) {
            showError(error.message || "Erro ao criar agrupamento");
            console.error(error);
        } finally {
            setCreatingGrouping(false);
        }
    }

    const formatDate = (dateString: string) => {
        try {
            return new Date(dateString).toLocaleDateString("pt-BR", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
            });
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

    // Filtro de datas
    const renderDateFilter = () => (
        <div className="mb-4 p-4 bg-white">
            <div className="flex flex-wrap *:flex-grow items-end gap-6">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                        Tipo de Agrupamento
                    </label>
                    <select
                        className="px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                        value={groupBy}
                        onChange={(e) => setGroupBy(e.target.value as 'week' | 'month' | 'custom')}
                    >
                        <option value="custom">Intervalo Personalizado</option>
                        <option value="week">Semanal</option>
                        <option value="month">Mensal</option>
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                        Data Inicial
                    </label>
                    <input
                        type="date"
                        value={startDate}
                        disabled={groupBy !== 'custom'}
                        onChange={(e) => setStartDate(e.target.value)}
                        className={`px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 ${groupBy !== 'custom' ? 'bg-gray-100 cursor-not-allowed' : ''}`}
                    />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                        Data Final
                    </label>
                    <input
                        type="date"
                        value={endDate}
                        disabled={groupBy !== 'custom'}
                        onChange={(e) => setEndDate(e.target.value)}
                        className={`px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 ${groupBy !== 'custom' ? 'bg-gray-100 cursor-not-allowed' : ''}`}
                    />
                </div>
                <div>
                    <button
                        onClick={handleConfirmGrouping}
                        disabled={loading || purchases.length === 0 || creatingGrouping || !clientId}
                        className={`px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors ${
                            (loading || purchases.length === 0 || creatingGrouping || !clientId) 
                            ? 'opacity-50 cursor-not-allowed' 
                            : ''
                        }`}
                    >
                        {creatingGrouping ? "Criando..." : "Confirmar Agrupamento"}
                    </button>
                </div>
            </div>
        </div>
    );

    if (!clientId) {
        return (
            <div className="text-center py-12 text-gray-500">
                <p>Selecione um cliente para visualizar os arquivos de compra</p>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            <h2 className="text-lg font-semibold">Arquivos de Compra</h2>
            {/* Loading skeleton */}
            {loading ? (
                <div className="space-y-2">
                    {[...Array(5)].map((_, i) => (
                        <div key={i} className="h-16 bg-gray-200 animate-pulse rounded-lg" />
                    ))}
                </div>
            ) : purchases.length === 0 ? (
                <>
                    {renderDateFilter()}
                    <div className="text-center py-12 text-gray-500">
                        <p>Nenhum arquivo de compra encontrado</p>
                    </div>
                </>
            ) : (
                <>
                    {/* Tabela */}
                    {renderDateFilter()}
                    <div className="overflow-x-auto">
                        <table className="w-full border-collapse">
                            <thead>
                                <tr className="bg-gray-100 border-b border-gray-300">
                                    <th className="text-left p-3 font-semibold">Data da Compra</th>
                                    <th className="text-left p-3 font-semibold">Valor Total</th>
                                    <th className="text-left p-3 font-semibold">Última Atualização</th>
                                    <th className="text-center p-3 font-semibold">Ações</th>
                                </tr>
                            </thead>
                            <tbody>
                                {purchases.map((purchase) => (
                                    <tr
                                        key={purchase.id}
                                        className="border-b border-gray-300 hover:bg-gray-50 transition-colors"
                                    >
                                        <td className="p-3">{formatDate(purchase.purchaseDate)}</td>
                                        <td className="p-3 font-semibold">{formatCurrency(purchase.total)}</td>
                                        <td className="p-3">{formatDate(purchase.updatedAt)}</td>
                                        <td className="p-3">
                                            <div className="flex items-center justify-center gap-2">
                                                <button
                                                    onClick={() => handleViewProducts(purchase)}
                                                    className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                    title="Ver produtos"
                                                >
                                                    <Eye/>
                                                </button>
                                                <button
                                                    onClick={() => handleDelete(purchase.id)}
                                                    className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                                    title="Deletar"
                                                >
                                                    <Trash/>
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Paginação */}
                    {totalPages > 1 && (
                        <div className="flex justify-center items-center gap-2 mt-4">
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
            {showModal && selectedPurchase && (
                <InvoiceProductsModal
                    purchaseId={selectedPurchase.id}
                    onClose={() => {
                        setShowModal(false);
                        setSelectedPurchase(null);
                    }}
                    onUpdate={fetchPurchases}
                />
            )}
        </div>
    );
}
