"use client";

import ClientProductsTable from "@/components/modules/ClientProductsTable";
import ClientSelector from "@/components/modules/ClientSelector";
import ClientSummaryCards from "@/components/modules/ClientSummaryCards";
import EnhancedUploadNotes from "@/components/modules/EnhancedUploadNotes";
import { ClientSelectionInfo } from "@/types/clientType";
import { useEffect, useState } from "react";

export default function PurchasesPage() {
    const [selectedClient, setSelectedClient] = useState<ClientSelectionInfo | null>(null);
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

    const [groupBy, setGroupBy] = useState<'week' | 'month' | 'custom'>('custom');
    const [refreshKey, setRefreshKey] = useState(0);

    const handleUploadSuccess = () => {
        setRefreshKey((prev) => prev + 1);
    }

    function getWeekInterval() {
        const today = new Date();
        // Encontra a última segunda-feira antes de hoje
        let lastMonday = new Date(today);
        lastMonday.setDate(today.getDate() - ((today.getDay() + 6) % 7) - 7); // segunda passada
        lastMonday.setHours(0, 0, 0, 0);

        // Encontra o último sábado após a última segunda-feira
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
        // Mês anterior
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

    return (
        <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col min-h-full">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-800">Gerenciamento de Compras</h1>
                <p className="text-gray-600">Analise as compras agrupadas por semana, mês ou intervalo personalizado para facilitar a análise temporal</p>
            </div>

            <div className="flex flex-wrap gap-6 mb-8 h-fit">
                {/* Selecionar cliente */}
                <div className="bg-white rounded-lg shadow-sm p-4 flex-1">
                    <ClientSelector onClientSelect={setSelectedClient} />
                </div>

                {/* Upload de notas */}
                <EnhancedUploadNotes
                    clientId={selectedClient?.clientId}
                    onUploadSuccess={handleUploadSuccess}
                />
            </div>


            {/* Cards de resumo do cliente */}
            <ClientSummaryCards clientId={selectedClient?.clientId} refreshKey={refreshKey} />

            {/* Filtros */}
            <div className="mt-6 rounded-lg p-4 bg-white shadow-sm">
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
                            onClick={() => {
                                const now = new Date();
                                const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
                                const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
                                setStartDate(firstDay.toISOString().split('T')[0]);
                                setEndDate(lastDay.toISOString().split('T')[0]);
                            }}
                            disabled={groupBy !== 'custom'}
                            className={`px-4 w-full py-2 bg-[var(--primary-light)] text-white rounded-lg hover:bg-[var(--primary-dark)] cursor-pointer transition-colors ${groupBy !== 'custom' ? 'opacity-50 cursor-not-allowed hover:bg-[var(--primary-light)]' : ''}`}
                        >
                            Mês Atual
                        </button>
                    </div>
                </div>
            </div>

            {/* Tabela de produtos do cliente */}
            <ClientProductsTable
                clientId={selectedClient?.clientId}
                startDate={startDate ? `${startDate}T00:00:00` : undefined}
                endDate={endDate ? `${endDate}T23:59:59` : undefined}
                refreshKey={refreshKey}
            />

        </main>
    );
}