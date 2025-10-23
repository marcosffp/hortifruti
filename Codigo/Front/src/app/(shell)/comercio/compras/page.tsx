'use client';

import GroupedProductsTable from "@/components/modules/tables/GroupedProductsTable";
import ClientSelector from "@/components/modules/ClientSelector";
import ClientSummaryCards from "@/components/modules/ClientSummaryCards";
import EnhancedUploadNotes from "@/components/modules/EnhancedUploadNotes";
import { ClientSelectionInfo } from "@/types/clientType";
import { useState } from "react";
import PurchaseFilesTable from "@/components/modules/tables/PurchaseFilesTable";
import { FileText, Package } from "lucide-react";
import CombinedScoresCards from "@/components/modules/CombinedScoresCards";

export default function PurchasesPage() {
    const [selectedClient, setSelectedClient] = useState<ClientSelectionInfo | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);
    const [tab, setTab] = useState<"purchaseFiles" | "grouped">("purchaseFiles");

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
                {/* Container das tabs responsivo */}
                <div className="flex gap-2 flex-nowrap overflow-x-auto w-full scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100">
                    <button
                        className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors ${
                            tab === "purchaseFiles"
                                ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
                                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                        }`}
                        onClick={() => setTab("purchaseFiles")}
                    >
                        <FileText className="w-5 h-5 mr-2" />
                        <span className="hidden lg:inline">Arquivos de Compra</span>
                        <span className="hidden md:inline lg:hidden">Arquivos</span>
                    </button>
                    <button
                        className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors ${
                            tab === "grouped"
                                ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
                                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                        }`}
                        onClick={() => setTab("grouped")}
                    >
                        <Package className="w-5 h-5 mr-2" />
                        <span className="hidden lg:inline">Produtos Agrupados</span>
                        <span className="hidden md:inline lg:hidden">Agrupados</span>
                    </button>
                </div>
                <div className="bg-white rounded-b-lg shadow-sm p-4 overflow-x-auto">
                    {tab === "purchaseFiles" && (
                        <div>
                            {/* Tabela de arquivos de compra */}
                            <PurchaseFilesTable
                                clientId={selectedClient?.clientId}
                                refreshKey={refreshKey}
                            />
                        </div>
                    )}
                    {tab === "grouped" && (
                        <CombinedScoresCards clientId={selectedClient?.clientId} refreshKey={refreshKey} />
                    )}
                </div>
            </div>
        </main>
    );
}