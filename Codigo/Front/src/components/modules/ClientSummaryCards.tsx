import { CircleDollarSign, Package, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import ClientDetailModal from "@/components/modals/ClientDetailModal";
import { useClient } from "@/hooks/useClient";
import { useCountUp } from "@/hooks/useCountUp";
import type { ClientInfo } from "@/types/clientType";

interface ClientSummaryCardsProps {
  clientId: number | undefined;
  refreshKey?: number;
}

export default function ClientSummaryCards({
  clientId,
  refreshKey,
}: ClientSummaryCardsProps) {
  const [clientInfo, setClientInfo] = useState<ClientInfo | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showDetails, setShowDetails] = useState(false);

  const animatedTotalProducts = useCountUp(clientInfo?.totalProducts ?? 0);
  const animatedTotalValue = useCountUp(clientInfo?.totalValue ?? 0);
  const { getClientSummary } = useClient();

  // biome-ignore lint/correctness/useExhaustiveDependencies: refreshKey is intentionally unused inside the effect — it only exists to force a refetch when the parent bumps it
  useEffect(() => {
    const fetchClientSummary = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getClientSummary(clientId as number);
        setClientInfo(data);
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : "Erro ao buscar dados do cliente",
        );
      } finally {
        setIsLoading(false);
      }
    };

    if (clientId) fetchClientSummary();
  }, [clientId, refreshKey, getClientSummary]);

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[...Array(3)].map((_, i) => (
          <div
            // biome-ignore lint/suspicious/noArrayIndexKey: static-length skeleton placeholder list, no stable identity available
            key={i}
            className="bg-gray-200 rounded-lg p-6 animate-pulse h-24"
          />
        ))}
      </div>
    );
  }

  if (error || !clientInfo) {
    return (
      <div className="bg-red-100 border border-red-300 rounded-lg p-4 text-center">
        {error || "Dados do cliente não encontrados."}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <button
        type="button"
        onClick={() => setShowDetails(true)}
        className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between text-left hover:shadow-md transition-shadow cursor-pointer"
      >
        <div className="flex flex-col items-start">
          <span className="text-gray-500 text-sm mb-2">Cliente</span>
          <span className="text-lg font-bold text-gray-800 hover:text-green-600 transition-colors">
            {clientInfo.clientName}
          </span>
          <span className="text-sm text-gray-600">
            {clientInfo.clientAddress}
          </span>
        </div>
        <div className="icon-3d-wrapper">
          <UserRound
            size={32}
            className="text-green-600 animate-icon-3d-spin"
          />
        </div>
      </button>

      <ClientDetailModal
        clientId={showDetails ? (clientId ?? null) : null}
        onClose={() => setShowDetails(false)}
      />
      <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
        <div className="flex flex-col items-start">
          <span className="text-gray-500 text-sm mb-2">Total de Produtos</span>
          <span
            key={clientInfo.totalProducts}
            className="text-lg font-bold animate-value-flash"
          >
            {Math.round(animatedTotalProducts).toLocaleString("pt-BR")}
          </span>
        </div>
        <div className="icon-3d-wrapper">
          <Package size={32} className="text-green-600 animate-icon-3d-spin" />
        </div>
      </div>
      <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
        <div className="flex flex-col items-start">
          <span className="text-gray-500 text-sm mb-2">Valor Total</span>
          <span
            key={clientInfo.totalValue}
            className="text-lg font-bold animate-value-flash"
          >
            R${" "}
            {animatedTotalValue.toLocaleString("pt-BR", {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}
          </span>
        </div>
        <div className="icon-3d-wrapper">
          <CircleDollarSign
            size={32}
            className="text-green-600 animate-icon-3d-spin"
          />
        </div>
      </div>
    </div>
  );
}
