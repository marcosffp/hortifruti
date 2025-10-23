import { useEffect, useState } from "react";
import SkeletonTableLoading from "@/components/ui/SkeletonTableLoading";
import { CombinedScoreType } from "@/types/combinedScoreType";
import { BadgeCheck, FileBadge, Trash, Plus } from "lucide-react";
import { showError, showSuccess } from "@/services/notificationService";
import ClientNumberModal from "@/components/modals/ClientNumberModal";
import { useBillet } from "@/hooks/useBillet";
import { combinedScoreService } from "@/services/combinedScoreService";
import { toast } from "react-toastify";

interface GroupedProductsTableProps {
  clientId: number | undefined;
  refreshKey?: number;
}

export default function GroupedProductsTable({ clientId, refreshKey }: GroupedProductsTableProps) {
  const [grouped, setGrouped] = useState<CombinedScoreType[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [clientNumberModal, setClientNumberModal] = useState({ state: false, groupId: -1 });
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [creatingGrouping, setCreatingGrouping] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { generateBillet } = useBillet();

  // Labels das colunas
  const columnLabels: Record<string, string> = {
    number: "Número",
    totalValue: "Valor Total",
    status: "Status",
    dueDate: "Data de Vencimento",
    confirmedAt: "Confirmado Em",
    actions: "Ações",
  };

  useEffect(() => {
    if (!clientId) return;

    const loadData = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const result = await combinedScoreService.fetchCombinedScores(clientId, page, 10);
        setGrouped(result.content || []);
        setTotalPages(result.totalPages || 1);
      } catch (err: any) {
        setError(err.message || "Erro ao buscar agrupamentos");
        console.error(err);
      } finally {
        setIsLoading(false);
      }
    };

    loadData();
  }, [clientId, refreshKey, page]);

  const handlePaymentConfirmation = async (groupId: number) => {
    setIsLoading(true);
    try {
      const res = await combinedScoreService.confirmPayment(groupId);
      setGrouped((prev) =>
        prev.map((item) =>
          item.id === groupId ? { ...item, status: "PAID" } : item
        )
      );
      showSuccess(res);
    } catch (err: any) {
      showError(err.message);
    } finally {
      setIsLoading(false);
    }
  }

  const handleCancelGrouping = async (groupId: number) => {
    setIsLoading(true);
    try {
      const res = await combinedScoreService.cancelGrouping(groupId);
      setGrouped((prev) => prev.filter((item) => item.id !== groupId));
      showSuccess(res);
    } catch (err: any) {
      showError(err.message);
    } finally {
      setIsLoading(false);
    }
  }

  const handleGenerateBillet = async (groupId: number, clientNumber: string) => {
    if (!clientNumber) {
      return showError("Número do cliente é obrigatório para gerar o boleto.");
    }

    generateBillet(groupId, clientNumber)
      .then((res) => {
        showSuccess(res);
      })
      .catch((err) => {
        showError(err.message);
      });
  }

  if (error) {
    return (
      <div>
        <div className="bg-red-100 border border-red-300 rounded-lg p-4 text-center mt-6">
          {error}
        </div>
      </div>
    );
  }

  if (isLoading) {
    return <SkeletonTableLoading title="Produtos Agrupados" cols={Object.values(columnLabels)} />;
  }

  if (!grouped.length) {
    return (
      <div className="text-gray-800">
        <h2 className="text-lg font-semibold text-gray-800">Produtos Agrupados</h2>
        <p className="text-center text-gray-500 mt-2">Nenhum grupo de produtos encontrado para este cliente.</p>
      </div>
    );
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-800 mb-4">Produtos Agrupados</h2>
      <table className="min-w-full border border-gray-200">
        <thead>
          <tr>
            {Object.entries(columnLabels).map(([col, label]) => (
              <th key={col} className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700">
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {grouped.map((row, idx) => (
            <tr key={row.id || idx}>
              {Object.keys(columnLabels).map((col) => {
                if (col === "actions") {
                  return (
                    <td key={col} className="px-4 py-2 border-b border-gray-100">
                      <button
                        title="Confirmar pagamento"
                        className="mr-2 text-green-600 hover:text-green-800 transition-colors"
                        onClick={() => handlePaymentConfirmation(row.id)}
                      >
                        <BadgeCheck></BadgeCheck>
                      </button>
                      <button
                        title="Cancelar agrupamento"
                        className="text-red-500 hover:text-red-800 transition-colors"
                        onClick={() => handleCancelGrouping(row.id)}
                      >
                        <Trash></Trash>
                      </button>
                      <button
                        title="Gerar Boleto"
                        className="ml-2 text-yellow-600 hover:text-yellow-800 transition-colors"
                        onClick={() => setClientNumberModal({ state: true, groupId: row.id })}
                      >
                        <FileBadge></FileBadge>
                      </button>
                    </td>
                  );
                }

                let value = row[col as keyof CombinedScoreType];

                switch (col) {
                  case "totalValue":
                    value = `R$ ${Number(value).toLocaleString("pt-BR", {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}`;
                    break;
                  case "dueDate":
                    if (!value) {
                      return (
                        <td key={col} className="px-4 py-2 border-b border-gray-100">
                          <span className="inline-block px-2 py-1 rounded-2xl border border-gray-300 bg-gray-100 text-gray-500 text-xs">
                            Não definida
                          </span>
                        </td>
                      );
                    } else {
                      value = new Date(value as string).toLocaleDateString("pt-BR");
                    }
                    break;
                  case "confirmedAt":
                    if (value) {
                      value = new Date(value as string).toLocaleString("pt-BR");
                    }
                    break;
                  case "status":
                    const statusColors: Record<string, string> = {
                      PAID: "border-green-300 bg-green-100 text-green-700",
                      PENDING: "border-yellow-300 bg-yellow-100 text-yellow-700",
                      OVERDUE: "border-red-300 bg-red-100 text-red-700",
                      CANCELLED: "border-gray-300 bg-gray-100 text-gray-700",
                    };
                    const statusLabels: Record<string, string> = {
                      PAID: "Pago",
                      PENDING: "Pendente",
                      OVERDUE: "Vencido",
                      CANCELLED: "Cancelado",
                    };
                    return (
                      <td key={col} className="px-4 py-2 border-b border-gray-100">
                        <span className={`inline-block px-2 py-1 rounded-2xl border text-xs ${statusColors[value as string] || "border-gray-300 bg-gray-100 text-gray-500"}`}>
                          {statusLabels[value as string] || value}
                        </span>
                      </td>
                    );
                  default:
                    break;
                }

                return (
                  <td key={col} className="px-4 py-2 border-b border-gray-100">
                    {value}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <div className="flex justify-center items-center gap-2 mt-4">
        <button
          className="px-3 py-1 rounded bg-gray-100 hover:bg-gray-200 disabled:opacity-50"
          onClick={() => setPage((p) => Math.max(p - 1, 0))}
          disabled={page === 0}
        >
          Anterior
        </button>
        <span className="text-sm text-gray-700">
          Página {page + 1} de {totalPages}
        </span>
        <button
          className="px-3 py-1 rounded bg-gray-100 hover:bg-gray-200 disabled:opacity-50"
          onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
          disabled={page >= totalPages - 1}
        >
          Próxima
        </button>
      </div>

      <ClientNumberModal
        open={clientNumberModal.state}
        onClose={() => setClientNumberModal({ state: false, groupId: -1 })}
        onConfirm={(number) => { setClientNumberModal({ state: false, groupId: -1 }); handleGenerateBillet(clientNumberModal.groupId, number); }}
      />
    </div>
  );
}