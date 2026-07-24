"use client";

import { AlertTriangle, ExternalLink, FileText, Package } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import ManualCancelModal from "@/components/modals/ManualCancelModal";
import ClientSelector from "@/components/modules/ClientSelector";
import ClientSummaryCards from "@/components/modules/ClientSummaryCards";
import CombinedScoresCards from "@/components/modules/CombinedScoresCards";
import EnhancedUploadNotes from "@/components/modules/EnhancedUploadNotes";
import PurchaseFilesTable from "@/components/modules/tables/PurchaseFilesTable";
import { clientService } from "@/services/clientService";
import type { ClientSelectionInfo } from "@/types/clientType";

export default function PurchasesPage() {
  const searchParams = useSearchParams();
  const [selectedClient, setSelectedClient] =
    useState<ClientSelectionInfo | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [tab, setTab] = useState<"purchaseFiles" | "grouped">("purchaseFiles");
  const [showManualCancelModal, setShowManualCancelModal] = useState(false);

  const handleUploadSuccess = () => {
    setRefreshKey((prev) => prev + 1);
  };

  // Permite chegar direto no agrupamento de um cliente via link (ex: vindo da tela de Boletos)
  useEffect(() => {
    const clientIdParam = searchParams?.get("clientId");
    const tabParam = searchParams?.get("tab");

    if (tabParam === "grouped") {
      setTab("grouped");
    }

    if (!clientIdParam) return;
    const clientId = Number(clientIdParam);
    if (Number.isNaN(clientId)) return;

    clientService
      .getClientById(clientId)
      .then((client) => {
        setSelectedClient({
          clientId: client.id,
          clientName: client.clientName,
        });
      })
      .catch((error) => {
        console.error("Erro ao carregar cliente a partir do link:", error);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  return (
    <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col min-h-full">
      <div className="mb-8 flex flex-wrap items-start justify-between gap-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">
            Gerenciamento de Compras
          </h1>
          <p className="text-gray-600">
            Analise as compras agrupadas por semana, mês ou intervalo
            personalizado para facilitar a análise temporal
          </p>
        </div>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <button
            type="button"
            onClick={() =>
              window.open(
                process.env.NEXT_PUBLIC_LINK_PLANILHA_NOTINHAS ||
                  "https://docs.google.com/spreadsheets/d/1urqNj0ccGktQn_EvYnow-CIqCuWudSSYEtsfEcFOTKQ/edit?gid=516854495#gid=516854495",
                "_blank",
              )
            }
            className="flex items-center gap-1.5 px-3 py-2 bg-white text-gray-700 border border-gray-200 rounded-lg hover:bg-gray-50 hover:border-gray-300 transition-colors text-sm font-medium cursor-pointer"
          >
            <ExternalLink className="w-4 h-4 text-blue-800/80" />
            Planilha de Preenchimento
          </button>
          <button
            type="button"
            onClick={() =>
              window.open(
                process.env.NEXT_PUBLIC_LINK_PLANILHA_LISTAS_MAIORES || "#",
                "_blank",
              )
            }
            className="flex items-center gap-1.5 px-3 py-2 bg-white text-gray-700 border border-gray-200 rounded-lg hover:bg-gray-50 hover:border-gray-300 transition-colors text-sm font-medium cursor-pointer"
          >
            <ExternalLink className="w-4 h-4 text-blue-800/80" />
            Planilha de Listas Maiores
          </button>
          <button
            type="button"
            onClick={() => setShowManualCancelModal(true)}
            className="flex items-center gap-1.5 px-3 py-2 bg-white text-red-700 border border-gray-200 rounded-lg hover:bg-red-50 hover:border-red-200 transition-colors text-sm font-medium cursor-pointer"
          >
            <AlertTriangle className="w-4 h-4" />
            Cancelamento Manual (NF)
          </button>
        </div>
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

      <ClientSummaryCards
        clientId={selectedClient?.clientId}
        refreshKey={refreshKey}
      />

      <div className="mt-8">
        <div className="flex gap-2 flex-nowrap overflow-x-auto w-full scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100">
          <button
            type="button"
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
            type="button"
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
              <PurchaseFilesTable
                clientId={selectedClient?.clientId}
                refreshKey={refreshKey}
              />
            </div>
          )}
          {tab === "grouped" && (
            <CombinedScoresCards
              clientId={selectedClient?.clientId}
              refreshKey={refreshKey}
            />
          )}
        </div>
      </div>

      <ManualCancelModal
        open={showManualCancelModal}
        onClose={() => setShowManualCancelModal(false)}
      />
    </main>
  );
}
