"use client";

import {
  Search,
  Download,
  ArrowUp,
  ArrowDown,
  Edit,
  Trash2,
  ArrowLeft,
  ArrowRight,
  X,
  Upload,
  Wallet
} from "lucide-react";
import { useEffect, useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useTransaction } from "@/hooks/useTransaction";
import { showError, showSuccess } from "@/services/notificationService";
import { PageResult, TransactionResponse, TransactionRequest } from "@/services/transactionService";
import Button from "@/components/ui/Button";
import { getErrorMessage } from "@/types/errorType";
import Loading from "@/components/ui/Loading";

export default function FinancialLaunchesPage() {
  const router = useRouter();
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
  const [currentTransaction, setCurrentTransaction] = useState<TransactionResponse | null>(null);

  // Novos estados para o filtro de data
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

  const fetchSummaryData = async () => {
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
  };

  const fetchTransactionsData = async () => {
    try {
      const categories = await getAllCategories();
      setCategories(categories);

      const allTransactions: PageResult<TransactionResponse> | undefined =
        await getAllTransactions(search, type, category, page);
      setTransactions(allTransactions?.content || []);
      setTotalPages(allTransactions?.totalPages || 1);
    } catch (err) {
      console.error("Erro ao buscar transações: ", err);
    }
  };

  // Effect para dados do resumo (reage às mudanças de data)
  useEffect(() => {
    fetchSummaryData();
  }, [startDate, endDate]);

  // Effect para dados das transações (reage aos filtros de pesquisa)
  useEffect(() => {
    fetchTransactionsData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search, type, category, page]);

  const handleDelete = async (id: number) => {
    if (window.confirm("Tem certeza que deseja excluir este lançamento?")) {
      try {
        await deleteTransaction(id);
        alert("Lançamento excluído com sucesso!");
        fetchSummaryData(); // Refetch data after deletion
      } catch (err) {
        alert("Erro ao excluir lançamento: " + getErrorMessage(err));
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
      console.log("Enviando requisição de atualização:", formData);
      await updateTransaction(currentTransaction.id, formData);
      alert("Lançamento atualizado com sucesso!");
      setIsEditModalOpen(false);
      fetchSummaryData(); // Refetch data after update
    } catch (err) {
      console.error("Erro detalhado:", err);
      alert("Erro ao atualizar lançamento: " + getErrorMessage(err));
    }
  };

  const handleExport = async () => {
    try {
      await exportTransactionsAsExcel();
      showSuccess("Exportação realizada com sucesso!");
    } catch (err) {
      showError("Erro ao exportar lançamentos: " + getErrorMessage(err));
    }
  };
  
  const navigateToUpload = () => {
    router.push('/financeiro/upload');
  };

  return (
    <main className="flex-1 p-6 bg-gray-50 overflow-auto flex flex-col">
      {/* Header Section */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800">
          Lançamentos Financeiros
        </h1>
        <p className="text-gray-600">
          Gerencie todos os lançamentos financeiros do sistema
        </p>
      </div>

      {/* Date Range Filter */}
      <div className="bg-white rounded-lg shadow-sm p-4 mb-6">
        <h3 className="text-lg font-medium text-gray-800 mb-3">
          Filtro de Período (Resumo Financeiro)
        </h3>
        <div className="flex space-x-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Data Inicial
            </label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
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
              className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          <div className="flex items-end">
            <button
              onClick={() => {
                const now = new Date();
                const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
                const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
                setStartDate(firstDay.toISOString().split('T')[0]);
                setEndDate(lastDay.toISOString().split('T')[0]);
              }}
              className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Mês Atual
            </button>
          </div>
        </div>
      </div>

      {/* Summary Cards Section */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {/* Total de Entradas Card */}
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

        {/* Total de Saídas Card */}
        <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-500">Total de Saídas</p>
            {isLoading ? (
              <h2 className="text-2xl font-bold text-red-600"><Loading /></h2>
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

        {/* Saldo Total Card */}
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

      {/* Launches List Section */}
      <div className="bg-white rounded-lg shadow-sm p-6 flex-grow flex flex-col">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h2 className="text-xl font-semibold text-gray-800">
              Lista de Lançamentos
            </h2>
            <p className="text-sm text-gray-500">
              {transactions?.length || 0} lançamento(s) encontrado(s)
            </p>
          </div>
          <div className="flex space-x-4">
            <Button 
              variant="primary"
              onClick={navigateToUpload}
              className="py-2 px-4 bg-green-600 hover:bg-green-700 transition-colors"
              icon={<Upload size={18} />}
            >
              Importar Extrato  
            </Button>
            <Button
              variant="outline"
              onClick={handleExport}
              disabled={isLoading}
              className="border border-gray-300 text-gray-700 px-4 py-2"
              icon={isLoading ? undefined : <Download size={18} />}
            >
              {isLoading ? "Exportando..." : "Exportar"}
            </Button>
          </div>
        </div>

        {/* Search and Filters */}
        <div className="flex items-center space-x-4 mb-6">
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

        {/* Table of Launches */}
        <div className="overflow-x-auto">
          {isLoading ? (
            <Loading />
          ) : error ? (
            <p>Erro ao carregar lançamentos: {error}</p>
          ) : transactions && transactions.length > 0 ? (
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
                  // Se vier "2024-09-10", force como local:
                  <tr key={transaction.id} className="border-b border-gray-100">
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
                        transaction.transactionType === "CREDITO" ? "+" : "-"
                      }R$ ${(transaction.amount || 0)
                        .toFixed(2)
                        .replace(".", ",")}`}
                    </td>
                    <td className="py-3 px-4">{transaction.bank}</td>
                    <td className="py-3 px-4 flex space-x-2">
                      <button 
                        className="text-gray-700 hover:text-gray-900"
                        onClick={() => handleEdit(transaction)}
                      >
                        <Edit size={18} />
                      </button>
                      <button
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
          ) : (
            <p>Nenhum lançamento encontrado.</p>
          )}
        </div>
        <div className="mt-4 flex justify-start gap-5">
          <button
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg border ${
              page === 0
                ? "bg-gray-200 border-gray-300 cursor-not-allowed"
                : "bg-white border-gray-300 hover:bg-gray-100"
            } transition`}
          >
            <span
              className={`flex items-center justify-center w-8 h-8 rounded-full ${
                page === 0 ? "bg-gray-300" : "bg-gray-100 hover:bg-gray-200"
              }`}
            >
              <ArrowLeft
                size={20}
                className={page === 0 ? "text-gray-400" : "text-gray-700"}
              />
            </span>
            Anterior
          </button>
          <button
            disabled={page === totalPages - 1}
            onClick={() => setPage(page + 1)}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg border ${
              page === totalPages - 1
                ? "bg-gray-200 border-gray-300 cursor-not-allowed"
                : "bg-white border-gray-300 hover:bg-gray-100"
            } transition`}
          >
            Próxima
            <span
              className={`flex items-center justify-center w-8 h-8 rounded-full ${
                page === totalPages - 1
                  ? "bg-gray-300"
                  : "bg-gray-100 hover:bg-gray-200"
              }`}
            >
              <ArrowRight
                size={20}
                className={
                  page === totalPages - 1 ? "text-gray-400" : "text-gray-700"
                }
              />
            </span>
          </button>
        </div>
      </div>

      {/* Modal de Edição */}
      {isEditModalOpen && currentTransaction && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-xl">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-semibold">Editar Lançamento</h2>
              <button 
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
                  document: formData.get('document') as string || null,
                  history: formData.get('history') as string,
                  category: formData.get('category') as string,
                  transactionType: formData.get('transactionType') as "CREDITO" | "DEBITO",
                  transactionDate: formData.get('transactionDate') as string,
                  amount: parseFloat(formData.get('amount') as string),
                  bank: formData.get('bank') as string,
                  codHistory: formData.get('codHistory') as string,
                  batch: formData.get('batch') as string,
                  sourceAgency: formData.get('sourceAgency') as string,
                };
                
                handleUpdateTransaction(transaction);
              }}
            >
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Histórico
                  </label>
                  <input
                    type="text"
                    name="history"
                    defaultValue={currentTransaction.history}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Categoria
                  </label>
                  <select
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
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Tipo
                  </label>
                  <select
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
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Data
                  </label>
                  <input
                    type="date"
                    name="transactionDate"
                    defaultValue={currentTransaction.transactionDate}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Valor
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    name="amount"
                    defaultValue={currentTransaction.amount}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Banco
                  </label>
                  <input
                    type="text"
                    name="bank"
                    defaultValue={currentTransaction.bank}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Documento (opcional)
                  </label>
                  <input
                    type="text"
                    name="document"
                    defaultValue={currentTransaction.document || ''}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Código de Histórico*
                  </label>
                  <input
                    type="text"
                    name="codHistory"
                    defaultValue={currentTransaction.codHistory || '001'}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Lote*
                  </label>
                  <input
                    type="text"
                    name="batch"
                    defaultValue={currentTransaction.batch || '001'}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Agência de Origem*
                  </label>
                  <input
                    type="text"
                    name="sourceAgency"
                    defaultValue={currentTransaction.sourceAgency || '001'}
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
    </main>
  );
}
