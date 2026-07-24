"use client";

import {
  AlertCircle,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  CheckCircle2,
  Download,
  Edit,
  FileArchive,
  Info,
  RefreshCw,
  Search,
  Trash2,
  Wallet,
  X,
} from "lucide-react";
import { type FormEvent, useCallback, useEffect, useState } from "react";
import RoleGuard from "@/components/auth/RoleGuard";
import Button from "@/components/ui/Button";
import GameLoadingOverlay from "@/components/ui/GameLoadingOverlay";
import Loading from "@/components/ui/Loading";
import { useTransaction } from "@/hooks/useTransaction";
import {
  showError,
  showInfo,
  showSuccess,
} from "@/services/notificationService";
import { statementApiService } from "@/services/statementApiService";
import type {
  PageResult,
  TransactionRequest,
  TransactionResponse,
} from "@/services/transactionService";
import { getErrorMessage } from "@/types/errorType";

type BankGenerateStatus = "success" | "alreadyProcessed" | "error";

interface SicoobGenerateResult {
  status: BankGenerateStatus;
  message: string;
  mes: number;
  ano: number;
  diaInicial: number;
  diaFinal: number;
}

interface BBGenerateResult {
  status: BankGenerateStatus;
  message: string;
  dataInicio: string;
  dataFim: string;
}

// A API de extratos do BB rejeita datas futuras, então o fim do "mês atual"
// nunca deve passar de hoje.
function getCurrentMonthRange() {
  const now = new Date();
  const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const clampedEnd = lastDay < today ? lastDay : today;
  return {
    startDate: firstDay.toISOString().split("T")[0],
    endDate: clampedEnd.toISOString().split("T")[0],
  };
}

function triggerBlobDownload(blob: Blob, fileName: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", fileName);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export default function FinancialLaunchesPage() {
  const {
    isLoading,
    error,
    getTotalRevenue,
    getTotalExpenses,
    getTotalBalance,
    getAllTransactions,
    deleteTransaction,
    updateTransaction,
    exportTransactionsAsExcel,
    exportTransactionsComplete,
    getAllCategories,
  } = useTransaction();

  const [totalRevenue, setTotalRevenue] = useState(0);
  const [totalExpenses, setTotalExpenses] = useState(0);
  const [totalBalance, setTotalBalance] = useState(0);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [categories, setCategories] = useState<string[]>([
    "Todas as categorias",
  ]);
  const [search, setSearch] = useState("");
  const [type, setType] = useState("Todos os tipos");
  const [category, setCategory] = useState("Todas as categorias");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [currentTransaction, setCurrentTransaction] =
    useState<TransactionResponse | null>(null);
  const [isGeneratingExtratos, setIsGeneratingExtratos] = useState(false);
  const [sicoobResult, setSicoobResult] = useState<SicoobGenerateResult | null>(
    null,
  );
  const [bbResult, setBbResult] = useState<BBGenerateResult | null>(null);
  const [exportKind, setExportKind] = useState<"excel" | "complete" | null>(
    null,
  );

  const [startDate, setStartDate] = useState(
    () => getCurrentMonthRange().startDate,
  );

  const [endDate, setEndDate] = useState(() => getCurrentMonthRange().endDate);

  // biome-ignore lint/correctness/useExhaustiveDependencies: getTotalRevenue/getTotalExpenses/getTotalBalance are recreated on every render by useTransaction and are not part of the fetch identity
  const fetchSummaryData = useCallback(async () => {
    try {
      const revenue = await getTotalRevenue(startDate, endDate);
      setTotalRevenue(revenue || 0);

      const expenses = await getTotalExpenses(startDate, endDate);
      setTotalExpenses(expenses || 0);

      const balance = await getTotalBalance(startDate, endDate);
      setTotalBalance(balance || 0);
    } catch (err) {
      console.error("Erro ao buscar dados do resumo: ", err);
    }
  }, [startDate, endDate]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: getAllCategories/getAllTransactions are recreated on every render by useTransaction and are not part of the fetch identity
  const fetchTransactionsData = useCallback(async () => {
    try {
      const categories = await getAllCategories();
      setCategories(categories);

      const allTransactions: PageResult<TransactionResponse> | undefined =
        await getAllTransactions(search, type, category, page);

      if (allTransactions) {
        setTransactions(allTransactions.content || []);
        setTotalPages(allTransactions.totalPages || 1);

        if (
          page >= allTransactions.totalPages &&
          allTransactions.totalPages > 0
        ) {
          setPage(Math.max(0, allTransactions.totalPages - 1));
        }
      } else {
        setTransactions([]);
        setTotalPages(1);
      }
    } catch (err) {
      console.error("Erro ao buscar transações: ", err);
      setTransactions([]);
      setTotalPages(1);
    }
  }, [search, type, category, page]);

  useEffect(() => {
    fetchSummaryData();
  }, [fetchSummaryData]);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      fetchTransactionsData();
    }, 300); // Debounce de 300ms

    return () => clearTimeout(timeoutId);
  }, [fetchTransactionsData]);

  const handleDelete = async (id: number) => {
    if (window.confirm("Tem certeza que deseja excluir este lançamento?")) {
      try {
        await deleteTransaction(id);
        alert("Lançamento excluído com sucesso!");
        fetchSummaryData();
      } catch (err) {
        alert(`Erro ao excluir lançamento: ${getErrorMessage(err)}`);
      }
    }
  };

  const handleEdit = (transaction: TransactionResponse) => {
    setCurrentTransaction(transaction);
    setIsEditModalOpen(true);
  };

  const handleUpdateTransaction = async (formData: TransactionRequest) => {
    if (!currentTransaction) return;

    try {
      await updateTransaction(currentTransaction.id, formData);
      alert("Lançamento atualizado com sucesso!");
      setIsEditModalOpen(false);
      fetchSummaryData();
    } catch (err) {
      console.error("Erro detalhado:", err);
      alert(`Erro ao atualizar lançamento: ${getErrorMessage(err)}`);
    }
  };

  const handleExport = async () => {
    setExportKind("excel");
    try {
      await exportTransactionsAsExcel(startDate, endDate);
      showSuccess("Exportação Excel realizada com sucesso!");
    } catch (err) {
      showError(`Erro ao exportar lançamentos: ${getErrorMessage(err)}`);
    } finally {
      setExportKind(null);
    }
  };

  const handleExportComplete = async () => {
    setExportKind("complete");
    try {
      await exportTransactionsComplete(startDate, endDate);
      showSuccess("Exportação completa realizada com sucesso!");
    } catch (err) {
      showError(`Erro ao exportar relatório completo: ${getErrorMessage(err)}`);
    } finally {
      setExportKind(null);
    }
  };

  const [startYearStr, startMonthStr] = startDate.split("-");
  const [endYearStr, endMonthStr] = endDate.split("-");
  const isSameMonth =
    startYearStr === endYearStr && startMonthStr === endMonthStr;

  const summaryMessage = (
    alreadyProcessed: boolean,
    periodStart: string,
    periodEnd: string,
    totalSaved: number,
    totalDuplicatedSkipped: number,
  ) =>
    alreadyProcessed
      ? `Período já processado (${periodStart} a ${periodEnd}).`
      : `${totalSaved} lançamento(s) novo(s) salvo(s) (${totalDuplicatedSkipped} já existiam).`;

  const handleGenerateExtratos = async () => {
    if (!isSameMonth || isGeneratingExtratos) return;

    setIsGeneratingExtratos(true);
    setSicoobResult(null);
    setBbResult(null);

    const ano = Number(startYearStr);
    const mes = Number(startMonthStr);
    const diaInicial = Number(startDate.split("-")[2]);
    const diaFinal = Number(endDate.split("-")[2]);

    const [sicoobSettled, bbSettled] = await Promise.allSettled([
      statementApiService.importSicoob(mes, ano, diaInicial, diaFinal),
      statementApiService.importBB(startDate, endDate),
    ]);

    if (sicoobSettled.status === "fulfilled") {
      const summary = sicoobSettled.value;
      const message = summaryMessage(
        summary.alreadyProcessed,
        summary.periodStart,
        summary.periodEnd,
        summary.totalSaved,
        summary.totalDuplicatedSkipped,
      );
      setSicoobResult({
        status: summary.alreadyProcessed ? "alreadyProcessed" : "success",
        message,
        mes,
        ano,
        diaInicial,
        diaFinal,
      });
      if (summary.alreadyProcessed) {
        showInfo(`Sicoob: ${message}`);
      } else {
        showSuccess(`Sicoob: ${message}`);
      }
    } else {
      const message = getErrorMessage(sicoobSettled.reason);
      setSicoobResult({
        status: "error",
        message,
        mes,
        ano,
        diaInicial,
        diaFinal,
      });
      showError(`Sicoob: ${message}`);
    }

    if (bbSettled.status === "fulfilled") {
      const summary = bbSettled.value;
      const message = summaryMessage(
        summary.alreadyProcessed,
        summary.periodStart,
        summary.periodEnd,
        summary.totalSaved,
        summary.totalDuplicatedSkipped,
      );
      setBbResult({
        status: summary.alreadyProcessed ? "alreadyProcessed" : "success",
        message,
        dataInicio: startDate,
        dataFim: endDate,
      });
      if (summary.alreadyProcessed) {
        showInfo(`BB: ${message}`);
      } else {
        showSuccess(`BB: ${message}`);
      }
    } else {
      const message = getErrorMessage(bbSettled.reason);
      setBbResult({
        status: "error",
        message,
        dataInicio: startDate,
        dataFim: endDate,
      });
      showError(`BB: ${message}`);
    }

    setIsGeneratingExtratos(false);

    const anySucceeded =
      (sicoobSettled.status === "fulfilled" &&
        !sicoobSettled.value.alreadyProcessed) ||
      (bbSettled.status === "fulfilled" && !bbSettled.value.alreadyProcessed);
    if (anySucceeded) {
      fetchSummaryData();
      fetchTransactionsData();
    }
  };

  const handleDownloadSicoobPdf = async () => {
    if (!sicoobResult) return;
    try {
      const blob = await statementApiService.downloadSicoobPdf(
        sicoobResult.mes,
        sicoobResult.ano,
        sicoobResult.diaInicial,
        sicoobResult.diaFinal,
      );
      triggerBlobDownload(
        blob,
        `extrato-sicoob_${sicoobResult.ano}${String(sicoobResult.mes).padStart(2, "0")}.pdf`,
      );
    } catch (err) {
      showError(`Erro ao baixar PDF do Sicoob: ${getErrorMessage(err)}`);
    }
  };

  const handleDownloadSicoobExcel = async () => {
    if (!sicoobResult) return;
    try {
      const blob = await statementApiService.downloadSicoobExcel(
        sicoobResult.mes,
        sicoobResult.ano,
        sicoobResult.diaInicial,
        sicoobResult.diaFinal,
      );
      triggerBlobDownload(
        blob,
        `extrato-sicoob_${sicoobResult.ano}${String(sicoobResult.mes).padStart(2, "0")}.xlsx`,
      );
    } catch (err) {
      showError(`Erro ao baixar Excel do Sicoob: ${getErrorMessage(err)}`);
    }
  };

  const handleDownloadBBPdf = async () => {
    if (!bbResult) return;
    try {
      const blob = await statementApiService.downloadBBPdf(
        bbResult.dataInicio,
        bbResult.dataFim,
      );
      triggerBlobDownload(
        blob,
        `extrato-bb_${bbResult.dataInicio}_a_${bbResult.dataFim}.pdf`,
      );
    } catch (err) {
      showError(`Erro ao baixar PDF do BB: ${getErrorMessage(err)}`);
    }
  };

  const handleDownloadBBExcel = async () => {
    if (!bbResult) return;
    try {
      const blob = await statementApiService.downloadBBExcel(
        bbResult.dataInicio,
        bbResult.dataFim,
      );
      triggerBlobDownload(
        blob,
        `extrato-bb_${bbResult.dataInicio}_a_${bbResult.dataFim}.xlsx`,
      );
    } catch (err) {
      showError(`Erro ao baixar Excel do BB: ${getErrorMessage(err)}`);
    }
  };

  return (
    <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800">
          Lançamentos Financeiros
        </h1>
        <p className="text-gray-600">
          Gerencie todos os lançamentos financeiros do sistema
        </p>
      </div>

      <div className="flex flex-wrap gap-6 mb-8 h-fit">
        <div className="bg-white rounded-lg shadow-sm p-4 min-h-full w-full">
          <h3 className="text-lg font-medium text-gray-800 mb-3">
            Filtro de Período (Resumo e Exportação)
          </h3>
          <div className="flex flex-wrap items-end gap-4">
            <div>
              <label
                htmlFor="lancamentos-data-inicial"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Data Inicial
              </label>
              <input
                id="lancamentos-data-inicial"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>
            <div>
              <label
                htmlFor="lancamentos-data-final"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Data Final
              </label>
              <input
                id="lancamentos-data-final"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>
            <button
              type="button"
              onClick={() => {
                const range = getCurrentMonthRange();
                setStartDate(range.startDate);
                setEndDate(range.endDate);
              }}
              className="px-4 py-2 bg-[var(--primary-light)] text-white rounded-lg hover:bg-[var(--primary-dark)] cursor-pointer transition-colors"
            >
              Mês Atual
            </button>
            <RoleGuard roles="MANAGER" ignoreRedirect>
              <Button
                variant="outline"
                onClick={handleGenerateExtratos}
                disabled={!isSameMonth || isGeneratingExtratos}
                title={
                  !isSameMonth
                    ? "Selecione um período dentro de um único mês"
                    : undefined
                }
                className="border border-gray-300 text-gray-700 px-4 py-2"
                icon={
                  isGeneratingExtratos ? undefined : <RefreshCw size={18} />
                }
              >
                {isGeneratingExtratos ? "Gerando..." : "Gerar Extratos"}
              </Button>
            </RoleGuard>
          </div>
          {!isSameMonth && (
            <p className="text-xs text-amber-600 mt-2">
              A busca de extratos via API só funciona dentro de um único mês.
            </p>
          )}

          {(sicoobResult || bbResult) && (
            <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              {sicoobResult && (
                <div
                  className={`rounded-lg border p-4 ${
                    sicoobResult.status === "error"
                      ? "border-red-200 bg-red-50"
                      : sicoobResult.status === "alreadyProcessed"
                        ? "border-blue-200 bg-blue-50"
                        : "border-green-200 bg-green-50"
                  }`}
                >
                  <div className="flex items-center gap-2 mb-1">
                    {sicoobResult.status === "error" ? (
                      <AlertCircle className="text-red-500" size={18} />
                    ) : sicoobResult.status === "alreadyProcessed" ? (
                      <Info className="text-blue-500" size={18} />
                    ) : (
                      <CheckCircle2 className="text-green-500" size={18} />
                    )}
                    <span className="font-medium text-gray-800">Sicoob</span>
                  </div>
                  <p className="text-sm text-gray-600">
                    {sicoobResult.message}
                  </p>
                  {sicoobResult.status !== "error" && (
                    <div className="flex gap-2 mt-3">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleDownloadSicoobPdf}
                        icon={<Download size={16} />}
                      >
                        Baixar PDF
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleDownloadSicoobExcel}
                        icon={<Download size={16} />}
                      >
                        Baixar Excel
                      </Button>
                    </div>
                  )}
                </div>
              )}

              {bbResult && (
                <div
                  className={`rounded-lg border p-4 ${
                    bbResult.status === "error"
                      ? "border-red-200 bg-red-50"
                      : bbResult.status === "alreadyProcessed"
                        ? "border-blue-200 bg-blue-50"
                        : "border-green-200 bg-green-50"
                  }`}
                >
                  <div className="flex items-center gap-2 mb-1">
                    {bbResult.status === "error" ? (
                      <AlertCircle className="text-red-500" size={18} />
                    ) : bbResult.status === "alreadyProcessed" ? (
                      <Info className="text-blue-500" size={18} />
                    ) : (
                      <CheckCircle2 className="text-green-500" size={18} />
                    )}
                    <span className="font-medium text-gray-800">
                      Banco do Brasil
                    </span>
                  </div>
                  <p className="text-sm text-gray-600">{bbResult.message}</p>
                  {bbResult.status !== "error" && (
                    <div className="flex gap-2 mt-3">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleDownloadBBPdf}
                        icon={<Download size={16} />}
                      >
                        Baixar PDF
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleDownloadBBExcel}
                        icon={<Download size={16} />}
                      >
                        Baixar Excel
                      </Button>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-500">Total de Entradas</p>
            {isLoading ? (
              <h2 className="text-2xl font-bold text-green-600">
                <Loading />
              </h2>
            ) : error ? (
              <h2 className="text-2xl font-bold text-red-600">Erro</h2>
            ) : (
              <h2 className="text-2xl font-bold text-green-600">
                R${" "}
                {totalRevenue?.toLocaleString("pt-BR", {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </h2>
            )}
            <p className="text-xs text-gray-400">
              Período: {(() => {
                const [year, month, day] = startDate.split("-");
                const localStartDate = new Date(
                  Number(year),
                  Number(month) - 1,
                  Number(day),
                );
                return localStartDate.toLocaleDateString("pt-BR");
              })()} a {(() => {
                const [year, month, day] = endDate.split("-");
                const localEndDate = new Date(
                  Number(year),
                  Number(month) - 1,
                  Number(day),
                );
                return localEndDate.toLocaleDateString("pt-BR");
              })()}
            </p>
          </div>
          <ArrowUp className="text-green-500" size={24} />
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-500">Total de Saídas</p>
            {isLoading ? (
              <h2 className="text-2xl font-bold text-red-600">
                <Loading />
              </h2>
            ) : error ? (
              <h2 className="text-2xl font-bold text-red-600">Erro</h2>
            ) : (
              <h2 className="text-2xl font-bold text-red-600">
                R${" "}
                {totalExpenses?.toLocaleString("pt-BR", {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </h2>
            )}
            <p className="text-xs text-gray-400">
              Período: {(() => {
                const [year, month, day] = startDate.split("-");
                const localStartDate = new Date(
                  Number(year),
                  Number(month) - 1,
                  Number(day),
                );
                return localStartDate.toLocaleDateString("pt-BR");
              })()} a {(() => {
                const [year, month, day] = endDate.split("-");
                const localEndDate = new Date(
                  Number(year),
                  Number(month) - 1,
                  Number(day),
                );
                return localEndDate.toLocaleDateString("pt-BR");
              })()}
            </p>
          </div>
          <ArrowDown className="text-red-500" size={24} />
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-500">Saldo Total</p>
            {isLoading ? (
              <h2 className="text-2xl font-bold text-gray-800">
                <Loading />
              </h2>
            ) : error ? (
              <h2 className="text-2xl font-bold text-red-600">Erro</h2>
            ) : (
              <h2 className="text-2xl font-bold text-gray-800">
                R${" "}
                {totalBalance?.toLocaleString("pt-BR", {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </h2>
            )}
            <p className="text-xs text-gray-400">
              Período: {(() => {
                const [year, month, day] = startDate.split("-");
                const localStartDate = new Date(
                  Number(year),
                  Number(month) - 1,
                  Number(day),
                );
                return localStartDate.toLocaleDateString("pt-BR");
              })()} a {(() => {
                const [year, month, day] = endDate.split("-");
                const localEndDate = new Date(
                  Number(year),
                  Number(month) - 1,
                  Number(day),
                );
                return localEndDate.toLocaleDateString("pt-BR");
              })()}
            </p>
          </div>
          <Wallet className="text-gray-500" size={24} />
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm p-6 flex-grow flex flex-col">
        <div className="flex justify-between flex-wrap space-y-3 items-center mb-6">
          <div>
            <h2 className="text-xl font-semibold text-gray-800">
              Lista de Lançamentos
            </h2>
            <p className="text-sm text-gray-500">
              {transactions?.length || 0} lançamento(s) encontrado(s)
            </p>
          </div>
          <div className="flex gap-4 flex-wrap-reverse">
            <Button
              variant="outline"
              onClick={handleExport}
              disabled={exportKind !== null}
              className="border border-gray-300 text-gray-700 px-4 py-2"
              icon={exportKind === "excel" ? undefined : <Download size={18} />}
            >
              {exportKind === "excel" ? "Exportando..." : "Exportar Excel"}
            </Button>
            <Button
              variant="outline"
              onClick={handleExportComplete}
              disabled={exportKind !== null}
              className="border border-green-300 text-green-700 px-4 py-2 hover:bg-green-50"
              icon={
                exportKind === "complete" ? undefined : (
                  <FileArchive size={18} />
                )
              }
            >
              {exportKind === "complete"
                ? "Exportando..."
                : "Exportar Completo"}
            </Button>
          </div>
        </div>

        <div className="flex items-center gap-4 flex-wrap mb-6">
          <div className="relative flex-grow">
            <Search
              size={18}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            />
            <input
              type="text"
              placeholder="Buscar por histórico ou categoria..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          <select
            value={type}
            onChange={(e) => setType(e.target.value)}
            className="border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
          >
            <option>Todos os tipos</option>
            <option>Entrada</option>
            <option>Saída</option>
          </select>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-green-500"
          >
            <option>Todas as categorias</option>
            {categories.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>
        </div>

        <div>
          {isLoading ? (
            <Loading />
          ) : error ? (
            <p>Erro ao carregar lançamentos: {error}</p>
          ) : transactions && transactions.length > 0 ? (
            <>
              <div className="hidden md:block overflow-x-auto">
                <table className="min-w-full bg-white">
                  <thead>
                    <tr className="text-left text-gray-600 border-b border-gray-200">
                      <th className="py-3 px-4 font-semibold">Data</th>
                      <th className="py-3 px-4 font-semibold">Histórico</th>
                      <th className="py-3 px-4 font-semibold">Categoria</th>
                      <th className="py-3 px-4 font-semibold">Tipo</th>
                      <th className="py-3 px-4 font-semibold">Valor</th>
                      <th className="py-3 px-4 font-semibold">Banco</th>
                      <th className="py-3 px-4 font-semibold">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map((transaction) => (
                      <tr
                        key={transaction.id}
                        className="border-b border-gray-100"
                      >
                        <td className="py-3 px-4">
                          {(() => {
                            // Se vier "2024-09-10", force como local:
                            const [year, month, day] =
                              transaction.transactionDate.split("-");
                            const localDate = new Date(
                              Number(year),
                              Number(month) - 1,
                              Number(day),
                            );
                            return localDate.toLocaleDateString("pt-BR");
                          })()}
                        </td>
                        <td className="py-3 px-4">
                          {transaction.history.slice(0, 30)}...
                        </td>
                        <td className="py-3 px-4">
                          <span className="border border-[var(--neutral-300)] text-xs font-medium px-2.5 py-0.5 rounded-full ">
                            {transaction.category}
                          </span>
                        </td>
                        <td className="py-3 px-4">
                          <span
                            className={`text-xs font-medium px-2.5 py-0.5 rounded-full ${
                              transaction.transactionType === "CREDITO"
                                ? "bg-green-100 text-green-800"
                                : "bg-red-100 text-red-800"
                            }`}
                          >
                            {transaction.transactionType === "CREDITO"
                              ? "Entrada"
                              : "Saída"}
                          </span>
                        </td>
                        <td
                          className={`py-3 px-4 ${
                            transaction.transactionType === "CREDITO"
                              ? "text-green-600"
                              : "text-red-600"
                          }`}
                        >
                          {`${
                            transaction.transactionType === "CREDITO"
                              ? "+"
                              : "-"
                          }R$ ${(transaction.amount || 0)
                            .toFixed(2)
                            .replace(".", ",")}`}
                        </td>
                        <td className="py-3 px-4">{transaction.bank}</td>
                        <td className="py-3 px-4 flex space-x-2">
                          <button
                            type="button"
                            className="text-gray-700 hover:text-gray-900"
                            onClick={() => handleEdit(transaction)}
                          >
                            <Edit size={18} />
                          </button>
                          <button
                            type="button"
                            className="text-red-500 hover:text-red-700"
                            onClick={() => handleDelete(transaction.id)}
                            disabled={isLoading}
                          >
                            <Trash2 size={18} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="md:hidden space-y-3">
                {transactions.map((transaction) => {
                  const [year, month, day] =
                    transaction.transactionDate.split("-");
                  const localDate = new Date(
                    Number(year),
                    Number(month) - 1,
                    Number(day),
                  );
                  const isCredit = transaction.transactionType === "CREDITO";

                  return (
                    <div
                      key={transaction.id}
                      className="border border-gray-200 rounded-lg p-4"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="font-medium text-gray-800 break-words">
                            {transaction.history}
                          </p>
                          <p className="text-xs text-gray-500 mt-0.5">
                            {localDate.toLocaleDateString("pt-BR")} ·{" "}
                            {transaction.bank}
                          </p>
                        </div>
                        <p
                          className={`shrink-0 font-semibold ${
                            isCredit ? "text-green-600" : "text-red-600"
                          }`}
                        >
                          {`${isCredit ? "+" : "-"}R$ ${(
                            transaction.amount || 0
                          )
                            .toFixed(2)
                            .replace(".", ",")}`}
                        </p>
                      </div>

                      <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-100">
                        <div className="flex items-center gap-2">
                          <span className="border border-[var(--neutral-300)] text-xs font-medium px-2.5 py-0.5 rounded-full">
                            {transaction.category}
                          </span>
                          <span
                            className={`text-xs font-medium px-2.5 py-0.5 rounded-full ${
                              isCredit
                                ? "bg-green-100 text-green-800"
                                : "bg-red-100 text-red-800"
                            }`}
                          >
                            {isCredit ? "Entrada" : "Saída"}
                          </span>
                        </div>
                        <div className="flex items-center gap-3">
                          <button
                            type="button"
                            className="text-gray-700 hover:text-gray-900"
                            onClick={() => handleEdit(transaction)}
                            aria-label="Editar lançamento"
                          >
                            <Edit size={18} />
                          </button>
                          <button
                            type="button"
                            className="text-red-500 hover:text-red-700"
                            onClick={() => handleDelete(transaction.id)}
                            disabled={isLoading}
                            aria-label="Excluir lançamento"
                          >
                            <Trash2 size={18} />
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </>
          ) : (
            <p>Nenhum lançamento encontrado.</p>
          )}
        </div>
        <div className="mt-4 flex justify-start max-sm:justify-center gap-5">
          <button
            type="button"
            disabled={page === 0 || isLoading}
            onClick={() => {
              if (page > 0) {
                setPage(page - 1);
              }
            }}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg border ${
              page === 0 || isLoading
                ? "bg-gray-200 border-gray-300 cursor-not-allowed"
                : "bg-white border-gray-300 hover:bg-gray-100 cursor-pointer"
            } transition`}
          >
            <span
              className={`flex items-center justify-center w-8 h-8 rounded-full ${
                page === 0 || isLoading
                  ? "bg-gray-300"
                  : "bg-gray-100 hover:bg-gray-200"
              }`}
            >
              <ArrowLeft
                size={20}
                className={
                  page === 0 || isLoading ? "text-gray-400" : "text-gray-700"
                }
              />
            </span>
            Anterior
          </button>

          <div className="flex items-center px-4 py-2 bg-gray-100 rounded-lg">
            <span className="text-sm text-gray-600">
              Página {page + 1} de {Math.max(1, totalPages)}
              {transactions?.length > 0 &&
                ` (${transactions.length} resultados)`}
            </span>
          </div>

          <button
            type="button"
            disabled={page >= totalPages - 1 || totalPages <= 1 || isLoading}
            onClick={() => {
              if (page < totalPages - 1) {
                setPage(page + 1);
              }
            }}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg border ${
              page >= totalPages - 1 || totalPages <= 1 || isLoading
                ? "bg-gray-200 border-gray-300 cursor-not-allowed"
                : "bg-white border-gray-300 hover:bg-gray-100 cursor-pointer"
            } transition`}
          >
            Próxima
            <span
              className={`flex items-center justify-center w-8 h-8 rounded-full ${
                page >= totalPages - 1 || totalPages <= 1 || isLoading
                  ? "bg-gray-300"
                  : "bg-gray-100 hover:bg-gray-200"
              }`}
            >
              <ArrowRight
                size={20}
                className={
                  page >= totalPages - 1 || totalPages <= 1 || isLoading
                    ? "text-gray-400"
                    : "text-gray-700"
                }
              />
            </span>
          </button>
        </div>
      </div>

      {isEditModalOpen && currentTransaction && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-xl">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-semibold">Editar Lançamento</h2>
              <button
                type="button"
                className="text-gray-500 hover:text-gray-800"
                onClick={() => setIsEditModalOpen(false)}
              >
                <X size={24} />
              </button>
            </div>

            <form
              onSubmit={(e: FormEvent) => {
                e.preventDefault();
                const form = e.target as HTMLFormElement;
                const formData = new FormData(form);

                const transaction: TransactionRequest = {
                  document: (formData.get("document") as string) || null,
                  history: formData.get("history") as string,
                  category: formData.get("category") as string,
                  transactionType: formData.get("transactionType") as
                    | "CREDITO"
                    | "DEBITO",
                  transactionDate: formData.get("transactionDate") as string,
                  amount: parseFloat(formData.get("amount") as string),
                  bank: formData.get("bank") as string,
                  codHistory: formData.get("codHistory") as string,
                  batch: formData.get("batch") as string,
                  sourceAgency: formData.get("sourceAgency") as string,
                };

                handleUpdateTransaction(transaction);
              }}
            >
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <label
                    htmlFor="edit-history"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Histórico
                  </label>
                  <input
                    id="edit-history"
                    type="text"
                    name="history"
                    defaultValue={currentTransaction.history}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label
                    htmlFor="edit-category"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Categoria
                  </label>
                  <select
                    id="edit-category"
                    name="category"
                    defaultValue={currentTransaction.category}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  >
                    {categories.map((cat) => (
                      <option key={cat} value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label
                    htmlFor="edit-transactionType"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Tipo
                  </label>
                  <select
                    id="edit-transactionType"
                    name="transactionType"
                    defaultValue={currentTransaction.transactionType}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  >
                    <option value="CREDITO">Entrada</option>
                    <option value="DEBITO">Saída</option>
                  </select>
                </div>

                <div>
                  <label
                    htmlFor="edit-transactionDate"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Data
                  </label>
                  <input
                    id="edit-transactionDate"
                    type="date"
                    name="transactionDate"
                    defaultValue={currentTransaction.transactionDate}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label
                    htmlFor="edit-amount"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Valor
                  </label>
                  <input
                    id="edit-amount"
                    type="number"
                    step="0.01"
                    name="amount"
                    defaultValue={currentTransaction.amount}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label
                    htmlFor="edit-bank"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Banco
                  </label>
                  <input
                    id="edit-bank"
                    type="text"
                    name="bank"
                    defaultValue={currentTransaction.bank}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label
                    htmlFor="edit-document"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Documento (opcional)
                  </label>
                  <input
                    id="edit-document"
                    type="text"
                    name="document"
                    defaultValue={currentTransaction.document || ""}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label
                    htmlFor="edit-codHistory"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Código de Histórico*
                  </label>
                  <input
                    id="edit-codHistory"
                    type="text"
                    name="codHistory"
                    defaultValue={currentTransaction.codHistory || "001"}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label
                    htmlFor="edit-batch"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Lote*
                  </label>
                  <input
                    id="edit-batch"
                    type="text"
                    name="batch"
                    defaultValue={currentTransaction.batch || "001"}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label
                    htmlFor="edit-sourceAgency"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Agência de Origem*
                  </label>
                  <input
                    id="edit-sourceAgency"
                    type="text"
                    name="sourceAgency"
                    defaultValue={currentTransaction.sourceAgency || "001"}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>
              </div>

              <div className="flex justify-end space-x-4">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
                >
                  Salvar Alterações
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <GameLoadingOverlay
        isOpen={isGeneratingExtratos}
        title="Gerando extratos"
        messages={[
          "Conectando ao Sicoob e Banco do Brasil...",
          "Baixando movimentações do período...",
          "Conferindo lançamentos duplicados...",
          "Quase lá...",
        ]}
      />

      <GameLoadingOverlay
        isOpen={exportKind !== null}
        title={
          exportKind === "complete"
            ? "Gerando relatório completo"
            : "Exportando lançamentos"
        }
        messages={
          exportKind === "complete"
            ? [
                "Reunindo extratos e lançamentos...",
                "Montando planilhas e PDFs...",
                "Compactando o pacote final...",
                "Quase lá...",
              ]
            : [
                "Filtrando lançamentos do período...",
                "Montando a planilha Excel...",
                "Quase lá...",
              ]
        }
      />
    </main>
  );
}
