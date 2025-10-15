"use client";

import ClientProductsTable from "@/components/modules/ClientProductsTable";
import GroupedProductsTable from "@/components/modules/GroupedProductsTable";
import BilletsTable from "@/components/modules/BilletsTable";
import NotesTable from "@/components/modules/NotesTable";
import ClientSelector from "@/components/modules/ClientSelector";
import ClientSummaryCards from "@/components/modules/ClientSummaryCards";
import EnhancedUploadNotes from "@/components/modules/EnhancedUploadNotes";
import { ClientSelectionInfo } from "@/types/clientType";
import { useState } from "react";

export default function PurchasesPage() {
    const [selectedClient, setSelectedClient] = useState<ClientSelectionInfo | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);
    const [tab, setTab] = useState<"products" | "grouped" | "notes" | "boletos">("products");

    const handleUploadSuccess = () => {
        setRefreshKey((prev) => prev + 1);
    };

    return (
        <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col min-h-full">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-800">Gerenciamento de Compras</h1>
                <p className="text-gray-600">Analise as compras agrupadas por semana, mês ou intervalo personalizado para facilitar a análise temporal</p>
            </div>

            <div className="flex flex-wrap gap-6 mb-8 h-fit">
                <div className="bg-white rounded-lg shadow-sm p-4 flex-1">
                    <ClientSelector onClientSelect={setSelectedClient} />
                </div>
                <EnhancedUploadNotes
                    clientId={selectedClient?.clientId}
                    onUploadSuccess={handleUploadSuccess}
                />
            </div>

            <ClientSummaryCards clientId={selectedClient?.clientId} refreshKey={refreshKey} />

            {/* Tabs */}
            <div className="mt-8">
                <div className="flex gap-2">
                    <button
                        className={`px-4 py-2 rounded-t-lg font-medium transition-colors ${tab === "products"
                                ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
                                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                            }`}
                        onClick={() => setTab("products")}
                    >
                        Produtos
                    </button>
                    <button
                        className={`px-4 py-2 rounded-t-lg font-medium transition-colors ${tab === "grouped"
                                ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
                                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                            }`}
                        onClick={() => setTab("grouped")}
                    >
                        Produtos Agrupados
                    </button>
                    <button
                        className={`px-4 py-2 rounded-t-lg font-medium transition-colors ${tab === "notes"
                                ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
                                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                            }`}
                        onClick={() => setTab("notes")}
                    >
                        Notas Fiscais
                    </button>
                    <button
                        className={`px-4 py-2 rounded-t-lg font-medium transition-colors ${tab === "boletos"
                                ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
                                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                            }`}
                        onClick={() => setTab("boletos")}
                    >
                        Boletos
                    </button>
                </div>
                <div className="bg-white rounded-b-lg shadow-sm p-4 overflow-x-auto">
                    {tab === "products" && (
                        <ClientProductsTable
                            clientId={selectedClient?.clientId}
                            refreshKey={refreshKey}
                        />
                    )}
                    {tab === "grouped" && (
                        <GroupedProductsTable 
                            clientId={selectedClient?.clientId} 
                            refreshKey={refreshKey} 
                        />
                    )}
                    {tab === "notes" && <NotesTable />}
                    {tab === "boletos" && <BilletsTable />}
                </div>
            </div>
        </main>
    );
}