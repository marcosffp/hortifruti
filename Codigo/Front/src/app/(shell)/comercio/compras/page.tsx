"use client";

import ClientProductsTable from "@/components/modules/ClientProductsTable";
import ClientSelector from "@/components/modules/ClientSelector";
import ClientSummaryCards from "@/components/modules/ClientSummaryCards";
import EnhancedUploadNotes from "@/components/modules/EnhancedUploadNotes";
import { ClientSelectionInfo } from "@/types/clientType";
import { useState } from "react";

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
                />
            </div>


            {/* Cards de resumo do cliente */}
            <ClientSummaryCards clientId={selectedClient?.clientId} />

            {/* Filtros */}
            <div className="mt-6 mb-4 rounded-lg p-4 bg-white shadow-sm">
                <div className="flex flex-wrap *:flex-grow items-end gap-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Tipo de Agrupamento
                        </label>
                        <select
                            className="px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                            disabled
                        >
                            <option>Intervalo Personalizado</option>
                            <option>Semanal (em breve)</option>
                            <option>Mensal (em breve)</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Data Inicial
                        </label>
                        <input
                            type="date"
                            value={startDate}
                            onChange={(e) => setStartDate(e.target.value)}
                            className="px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Data Final
                        </label>
                        <input
                            type="date"
                            value={endDate}
                            onChange={(e) => setEndDate(e.target.value)}
                            className="px-3 w-full py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
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
                            className="px-4 w-full py-2 bg-[var(--primary-light)] text-white rounded-lg hover:bg-[var(--primary-dark)] cursor-pointer transition-colors"
                        >
                            Mês Atual
                        </button>
                    </div>
                </div>
            </div>

            {/* Tabela de produtos do cliente */}
            <ClientProductsTable
                clientId={selectedClient?.clientId}
            />

        </main>
    );
}