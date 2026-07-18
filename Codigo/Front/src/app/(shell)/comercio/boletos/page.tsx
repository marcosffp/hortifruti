"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
    AlertCircle,
    CalendarClock,
    CheckCircle2,
    CircleCheck,
    Clock,
    ExternalLink,
    Filter,
    ListChecks,
    Receipt,
    RefreshCcw,
    Search,
    ShieldQuestion,
    UserSearch,
} from "lucide-react";
import ClientSelector from "@/components/modules/ClientSelector";
import { useBillet } from "@/hooks/useBillet";
import { showError, showSuccess } from "@/services/notificationService";
import { BilletResponse, OpenBilletResponse } from "@/types/billetType";
import { ClientSelectionInfo } from "@/types/clientType";

type Tab = "abertos" | "porCliente";

const SITUACAO_OPTIONS = [
    { value: "", label: "Todas as situações" },
    { value: "1", label: "Em aberto" },
    { value: "2", label: "Baixado" },
    { value: "3", label: "Liquidado" },
];

function formatCurrency(value: number | null | undefined): string {
    return new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL",
    }).format(value || 0);
}

function formatDate(dateString: string | null | undefined): string {
    if (!dateString) return "Não definida";
    try {
        const datePart = dateString.split("T")[0];
        const [year, month, day] = datePart.split("-");
        if (!year || !month || !day) return dateString;
        return `${day}/${month}/${year}`;
    } catch {
        return dateString;
    }
}

function daysUntil(dateString: string | null | undefined): number | null {
    if (!dateString) return null;
    const datePart = dateString.split("T")[0];
    const due = new Date(`${datePart}T00:00:00`);
    if (Number.isNaN(due.getTime())) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const diffMs = due.getTime() - today.getTime();
    return Math.round(diffMs / (1000 * 60 * 60 * 24));
}

function DueBadge({ dueDate }: { dueDate: string | null | undefined }) {
    const diff = daysUntil(dueDate);

    if (diff === null) {
        return (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-700">
                Sem vencimento
            </span>
        );
    }

    if (diff < 0) {
        return (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-red-100 text-red-800">
                <AlertCircle className="w-3 h-3" />
                Vencido há {Math.abs(diff)} {Math.abs(diff) === 1 ? "dia" : "dias"}
            </span>
        );
    }

    if (diff === 0) {
        return (
            <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-orange-100 text-orange-800">
                <Clock className="w-3 h-3" />
                Vence hoje
            </span>
        );
    }

    return (
        <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-blue-100 text-blue-800">
            <CalendarClock className="w-3 h-3" />
            Vence em {diff} {diff === 1 ? "dia" : "dias"}
        </span>
    );
}

function situacaoBadgeColor(situacao: string): string {
    const value = situacao.toLowerCase();
    if (value.includes("liquidado") || value.includes("pago")) {
        return "bg-green-100 text-green-800";
    }
    if (value.includes("baixado") || value.includes("cancelado")) {
        return "bg-red-100 text-red-800";
    }
    return "bg-blue-100 text-blue-800";
}

export default function BoletosPage() {
    const router = useRouter();
    const { getOpenBillets, getClientBillets, markBilletAsPaid, isLoading } = useBillet();

    const [tab, setTab] = useState<Tab>("abertos");

    // Aba "Em aberto"
    const [openBillets, setOpenBillets] = useState<OpenBilletResponse[]>([]);
    const [loadingOpen, setLoadingOpen] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");
    const [markingPaidId, setMarkingPaidId] = useState<number | null>(null);

    // Aba "Consultar por cliente"
    const [selectedClient, setSelectedClient] = useState<ClientSelectionInfo | null>(null);
    const [situacao, setSituacao] = useState("");
    const [dataInicio, setDataInicio] = useState("");
    const [dataFim, setDataFim] = useState("");
    const [clientBillets, setClientBillets] = useState<BilletResponse[] | null>(null);
    const [loadingClientBillets, setLoadingClientBillets] = useState(false);

    const fetchOpenBillets = async () => {
        setLoadingOpen(true);
        try {
            const data = await getOpenBillets();
            setOpenBillets(data);
        } catch (error) {
            showError("Não foi possível carregar os boletos em aberto");
            console.error(error);
        } finally {
            setLoadingOpen(false);
        }
    };

    useEffect(() => {
        fetchOpenBillets();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleMarkAsPaid = async (billet: OpenBilletResponse) => {
        if (
            !window.confirm(
                `Confirmar o pagamento do boleto de ${billet.clientName} (${formatCurrency(billet.totalValue)})? Ele deixará de aparecer na lista de boletos em aberto.`
            )
        ) {
            return;
        }
        setMarkingPaidId(billet.combinedScoreId);
        try {
            await markBilletAsPaid(billet.combinedScoreId);
            showSuccess("Pagamento confirmado com sucesso.");
            setOpenBillets((prev) =>
                prev.filter((b) => b.combinedScoreId !== billet.combinedScoreId)
            );
        } catch (error: any) {
            showError(error?.message || "Não foi possível confirmar o pagamento do boleto");
            console.error(error);
        } finally {
            setMarkingPaidId(null);
        }
    };

    const filteredOpenBillets = useMemo(() => {
        if (!searchTerm.trim()) return openBillets;
        const term = searchTerm.toLowerCase();
        return openBillets.filter((b) => b.clientName.toLowerCase().includes(term));
    }, [openBillets, searchTerm]);

    const handleSearchClientBillets = async (client: ClientSelectionInfo | null) => {
        if (!client) return;
        setLoadingClientBillets(true);
        try {
            const data = await getClientBillets(client.clientId, {
                codigoSituacao: situacao ? Number(situacao) : undefined,
                dataInicio: dataInicio || undefined,
                dataFim: dataFim || undefined,
            });
            setClientBillets(data);
        } catch (error) {
            showError("Não foi possível buscar os boletos do cliente");
            console.error(error);
        } finally {
            setLoadingClientBillets(false);
        }
    };

    const handleClientSelect = (client: ClientSelectionInfo) => {
        setSelectedClient(client);
        handleSearchClientBillets(client);
    };

    const handleApplyFilters = () => {
        handleSearchClientBillets(selectedClient);
    };

    const handleClearFilters = () => {
        setSituacao("");
        setDataInicio("");
        setDataFim("");
        if (selectedClient) {
            setLoadingClientBillets(true);
            getClientBillets(selectedClient.clientId, {})
                .then(setClientBillets)
                .catch((error) => {
                    showError("Não foi possível buscar os boletos do cliente");
                    console.error(error);
                })
                .finally(() => setLoadingClientBillets(false));
        }
    };

    const goToGrouping = (clientId: number) => {
        router.push(`/comercio/compras?clientId=${clientId}&tab=grouped`);
    };

    return (
        <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col min-h-full">
            <div className="mb-6">
                <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
                    <Receipt className="w-7 h-7 text-primary" />
                    Boletos
                </h1>
                <p className="text-gray-600">
                    Acompanhe os boletos em aberto dos clientes e consulte o histórico de cobranças.
                </p>
            </div>

            <div className="flex gap-2 flex-nowrap overflow-x-auto w-full">
                <button
                    className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors cursor-pointer ${
                        tab === "abertos"
                            ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
                            : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                    }`}
                    onClick={() => setTab("abertos")}
                >
                    <ListChecks className="w-5 h-5 mr-2" />
                    Boletos em Aberto
                </button>
                <button
                    className={`flex items-center px-4 py-2 rounded-t-lg font-medium transition-colors cursor-pointer ${
                        tab === "porCliente"
                            ? "bg-white shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1),0_-2px_4px_-2px_rgba(0,0,0,0.06)]"
                            : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                    }`}
                    onClick={() => setTab("porCliente")}
                >
                    <UserSearch className="w-5 h-5 mr-2" />
                    Consultar por Cliente
                </button>
            </div>

            <div className="bg-white rounded-b-lg rounded-tr-lg shadow-sm p-6 flex-1">
                {tab === "abertos" && (
                    <div>
                        <div className="flex flex-wrap justify-between items-center gap-3 mb-5">
                            <div>
                                <h2 className="text-lg font-semibold text-gray-800">
                                    Todos os boletos em aberto
                                </h2>
                                <p className="text-sm text-gray-500">
                                    Ordenados do vencimento mais próximo para o mais distante
                                </p>
                            </div>
                            <button
                                onClick={fetchOpenBillets}
                                className="flex items-center gap-2 px-3 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors cursor-pointer"
                            >
                                <RefreshCcw className="w-4 h-4" />
                                Atualizar
                            </button>
                        </div>

                        <div className="relative w-full max-w-md mb-5">
                            <input
                                type="text"
                                placeholder="Buscar por nome do cliente..."
                                className="pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg w-full focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 transition-all"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                            <Search className="absolute left-3 top-3 text-gray-400" size={18} />
                        </div>

                        {loadingOpen ? (
                            <div className="space-y-3">
                                {[...Array(5)].map((_, i) => (
                                    <div key={i} className="h-14 bg-gray-100 animate-pulse rounded-lg" />
                                ))}
                            </div>
                        ) : filteredOpenBillets.length === 0 ? (
                            <div className="text-center py-16 text-gray-500">
                                <CircleCheck className="w-12 h-12 mx-auto mb-3 text-green-400" />
                                <p className="text-lg font-medium text-gray-700">
                                    {searchTerm
                                        ? "Nenhum cliente encontrado com esse nome"
                                        : "Nenhum boleto em aberto no momento"}
                                </p>
                                {!searchTerm && (
                                    <p className="text-sm mt-1">Todos os clientes estão em dia com os pagamentos</p>
                                )}
                            </div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead>
                                        <tr className="text-left text-gray-500 border-b">
                                            <th className="py-3 px-3 font-semibold">Cliente</th>
                                            <th className="py-3 px-3 font-semibold">Agrupamento</th>
                                            <th className="py-3 px-3 font-semibold">Valor</th>
                                            <th className="py-3 px-3 font-semibold">Vencimento</th>
                                            <th className="py-3 px-3 font-semibold">Situação</th>
                                            <th className="py-3 px-3 font-semibold text-right">Ação</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {filteredOpenBillets.map((billet) => (
                                            <tr
                                                key={billet.combinedScoreId}
                                                className="border-b last:border-0 hover:bg-gray-50 transition-colors"
                                            >
                                                <td className="py-3 px-3 font-medium text-gray-800">
                                                    {billet.clientName}
                                                </td>
                                                <td className="py-3 px-3 text-gray-500">#{billet.combinedScoreId}</td>
                                                <td className="py-3 px-3">{formatCurrency(billet.totalValue)}</td>
                                                <td className="py-3 px-3">{formatDate(billet.dueDate)}</td>
                                                <td className="py-3 px-3">
                                                    <div className="flex items-center gap-1.5">
                                                        <DueBadge dueDate={billet.dueDate} />
                                                        {!billet.confirmadoNoSicoob && (
                                                            <span
                                                                title="Não foi possível confirmar essa situação diretamente com o Sicoob agora. Os dados podem estar desatualizados."
                                                                className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-yellow-100 text-yellow-800 cursor-help"
                                                            >
                                                                <ShieldQuestion className="w-3 h-3" />
                                                                Não confirmado
                                                            </span>
                                                        )}
                                                    </div>
                                                </td>
                                                <td className="py-3 px-3 text-right">
                                                    <div className="flex items-center justify-end gap-2">
                                                        <button
                                                            onClick={() => handleMarkAsPaid(billet)}
                                                            disabled={markingPaidId === billet.combinedScoreId}
                                                            className="inline-flex items-center gap-1 px-3 py-1.5 bg-green-700 text-white rounded-lg hover:bg-green-800 transition-colors text-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                                                        >
                                                            <CheckCircle2 className="w-3 h-3" />
                                                            {markingPaidId === billet.combinedScoreId
                                                                ? "Confirmando..."
                                                                : "Marcar como pago"}
                                                        </button>
                                                        <button
                                                            onClick={() => goToGrouping(billet.clientId)}
                                                            className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer"
                                                        >
                                                            <ExternalLink className="w-3 h-3" />
                                                            Ver Agrupamento
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                )}

                {tab === "porCliente" && (
                    <div>
                        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,20rem)_1fr] gap-6">
                            <div className="bg-gray-50 border border-gray-200 rounded-lg">
                                <ClientSelector onClientSelect={handleClientSelect} />
                            </div>

                            <div>
                                <div className="flex items-center gap-2 mb-3 text-gray-700 font-medium">
                                    <Filter className="w-4 h-4" />
                                    Filtros
                                </div>
                                <div className="flex flex-wrap gap-3 items-end mb-5">
                                    <div className="flex flex-col gap-1">
                                        <label className="text-xs font-medium text-gray-600">Situação</label>
                                        <select
                                            className="border border-gray-300 rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                                            value={situacao}
                                            onChange={(e) => setSituacao(e.target.value)}
                                        >
                                            {SITUACAO_OPTIONS.map((opt) => (
                                                <option key={opt.value} value={opt.value}>
                                                    {opt.label}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="flex flex-col gap-1">
                                        <label className="text-xs font-medium text-gray-600">
                                            Vencimento de
                                        </label>
                                        <input
                                            type="date"
                                            className="border border-gray-300 rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                                            value={dataInicio}
                                            onChange={(e) => setDataInicio(e.target.value)}
                                        />
                                    </div>
                                    <div className="flex flex-col gap-1">
                                        <label className="text-xs font-medium text-gray-600">até</label>
                                        <input
                                            type="date"
                                            className="border border-gray-300 rounded-md p-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                                            value={dataFim}
                                            onChange={(e) => setDataFim(e.target.value)}
                                        />
                                    </div>
                                    <button
                                        onClick={handleApplyFilters}
                                        disabled={!selectedClient}
                                        className="flex items-center gap-2 px-4 py-2 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-sm disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                                    >
                                        <Search className="w-4 h-4" />
                                        Buscar
                                    </button>
                                    <button
                                        onClick={handleClearFilters}
                                        disabled={!selectedClient}
                                        className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                                    >
                                        Limpar filtros
                                    </button>
                                </div>

                                {!selectedClient && (
                                    <div className="text-center py-16 text-gray-500">
                                        <UserSearch className="w-12 h-12 mx-auto mb-3 opacity-40" />
                                        <p>Selecione um cliente para consultar os boletos</p>
                                    </div>
                                )}

                                {selectedClient && (loadingClientBillets || isLoading) && (
                                    <div className="space-y-3">
                                        {[...Array(4)].map((_, i) => (
                                            <div key={i} className="h-14 bg-gray-100 animate-pulse rounded-lg" />
                                        ))}
                                    </div>
                                )}

                                {selectedClient &&
                                    !loadingClientBillets &&
                                    clientBillets !== null &&
                                    clientBillets.length === 0 && (
                                        <div className="text-center py-16 text-gray-500">
                                            <Receipt className="w-12 h-12 mx-auto mb-3 opacity-40" />
                                            <p>Nenhum boleto encontrado para os filtros selecionados</p>
                                        </div>
                                    )}

                                {selectedClient &&
                                    !loadingClientBillets &&
                                    clientBillets !== null &&
                                    clientBillets.length > 0 && (
                                        <div className="overflow-x-auto">
                                            <table className="w-full text-sm">
                                                <thead>
                                                    <tr className="text-left text-gray-500 border-b">
                                                        <th className="py-3 px-3 font-semibold">Valor</th>
                                                        <th className="py-3 px-3 font-semibold">Agrupamento</th>
                                                        <th className="py-3 px-3 font-semibold">Emissão</th>
                                                        <th className="py-3 px-3 font-semibold">Vencimento</th>
                                                        <th className="py-3 px-3 font-semibold">Situação</th>
                                                        <th className="py-3 px-3 font-semibold text-right">Ação</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {clientBillets.map((billet, index) => (
                                                        <tr
                                                            key={`${billet.seuNumero}-${index}`}
                                                            className="border-b last:border-0 hover:bg-gray-50 transition-colors"
                                                        >
                                                            <td className="py-3 px-3 font-medium text-gray-800">
                                                                {formatCurrency(billet.valor)}
                                                            </td>
                                                            <td className="py-3 px-3 text-gray-500">
                                                                {billet.combinedScoreId ? `#${billet.combinedScoreId}` : "—"}
                                                            </td>
                                                            <td className="py-3 px-3">
                                                                {formatDate(billet.dataEmissao)}
                                                            </td>
                                                            <td className="py-3 px-3">
                                                                {formatDate(billet.dataVencimento)}
                                                            </td>
                                                            <td className="py-3 px-3">
                                                                <span
                                                                    className={`px-2 py-1 rounded text-xs font-medium ${situacaoBadgeColor(
                                                                        billet.situacaoBoleto
                                                                    )}`}
                                                                >
                                                                    {billet.situacaoBoleto}
                                                                </span>
                                                            </td>
                                                            <td className="py-3 px-3 text-right">
                                                                {billet.combinedScoreId ? (
                                                                    <button
                                                                        onClick={() =>
                                                                            selectedClient &&
                                                                            goToGrouping(selectedClient.clientId)
                                                                        }
                                                                        className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-800/80 text-white rounded-lg hover:bg-blue-800 transition-colors text-xs cursor-pointer"
                                                                    >
                                                                        <ExternalLink className="w-3 h-3" />
                                                                        Ver Agrupamento
                                                                    </button>
                                                                ) : (
                                                                    <span className="text-xs text-gray-400">
                                                                        Agrupamento não localizado
                                                                    </span>
                                                                )}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </main>
    );
}
