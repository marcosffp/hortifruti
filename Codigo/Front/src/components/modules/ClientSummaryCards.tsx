import { ClientInfo } from "@/types/clientType";
import { useEffect, useState } from "react";
import { UserRound, Package, CircleDollarSign } from "lucide-react";
import { getAuthHeaders } from "@/utils/httpUtils";

interface ClientSummaryCardsProps {
  clientId: number | undefined;
  refreshKey?: number;
}

export default function ClientSummaryCards({ clientId, refreshKey }: ClientSummaryCardsProps) {
  const [clientInfo, setClientInfo] = useState<ClientInfo | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchClientSummary = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await fetch(
          `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/clients/${clientId}/summary`, {
            headers: getAuthHeaders()
          }
        );
        if (!response.ok) throw new Error("Erro ao buscar dados do cliente");
        const data: ClientInfo = await response.json();
        setClientInfo(data);
      } catch (err: any) {
        setError(err.message || "Erro ao buscar dados do cliente");
      } finally {
        setIsLoading(false);
      }
    };

    if (clientId) fetchClientSummary();
  }, [clientId, refreshKey]);

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="bg-gray-200 rounded-lg p-6 animate-pulse h-24" />
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
      <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
        <div className="flex flex-col items-start">
          <span className="text-gray-500 text-sm mb-2">Cliente</span>
          <span className="text-lg font-bold text-gray-800">{clientInfo.clientName}</span>
            <span className="text-sm text-gray-600">{clientInfo.clientAddress}</span>
        </div>
        <UserRound size={32} className="text-green-600" />
      </div>
      <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
        <div className="flex flex-col items-start">
          <span className="text-gray-500 text-sm mb-2">Total de Produtos</span>
          <span className="text-lg font-bold">{clientInfo.totalProducts}</span>
        </div>
        <Package size={32} className="text-green-600" />
      </div>
      <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
        <div className="flex flex-col items-start">
          <span className="text-gray-500 text-sm mb-2">Valor Total</span>
          <span className="text-lg font-bold">
            R$ {clientInfo.totalValue.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}
          </span>
        </div>
        <CircleDollarSign size={32} className="text-green-600" />
      </div>
    </div>
  );
}