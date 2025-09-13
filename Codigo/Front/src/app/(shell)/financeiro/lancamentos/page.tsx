"use client";

import {
  Search,
  Plus,
  Download,
  ArrowUp,
  ArrowDown,
  Calendar,
  Edit,
  Trash2,
  ArrowLeft,
  ArrowRight,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useTransaction } from "@/hooks/useTransaction";
import { PageResult, TransactionResponse } from "@/services/transactionService";
import { getErrorMessage } from "@/utils/errorType";

export default function FinancialLaunchesPage() {
  const {
    isLoading,
    error,
    getTotalRevenue,
    getTotalExpenses,
    getTotalBalance,
    getAllTransactions,
    deleteTransaction,
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

  const fetchData = async () => {
    try {
      const revenue = await getTotalRevenue();
      setTotalRevenue(revenue || 0);

      const expenses = await getTotalExpenses();
      setTotalExpenses(expenses || 0);

      const balance = await getTotalBalance();
      setTotalBalance(balance || 0);

      const categories = await getAllCategories();
      setCategories(categories);

      const allTransactions: PageResult<TransactionResponse> | undefined =
        await getAllTransactions(search, type, category, page);
      setTransactions(allTransactions?.content || []);
      setTotalPages(allTransactions?.totalPages || 1);
    } catch (err) {
      console.error("Erro ao buscar dados: ", err);
    }
  };

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search, type, category, page]);

  const handleDelete = async (id: number) => {
    if (window.confirm("Tem certeza que deseja excluir este lançamento?")) {
      try {
        await deleteTransaction(id);
        alert("Lançamento excluído com sucesso!");
        fetchData(); // Refetch data after deletion
      } catch (err) {
        alert("Erro ao excluir lançamento: " + getErrorMessage(err));
      }
    }
  };

  const handleExport = async () => {
    try {
      await exportTransactionsAsExcel();
      alert("Exportação iniciada. Verifique seus downloads.");
    } catch (err) {
      alert("Erro ao exportar lançamentos: " + getErrorMessage(err));
    }
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

      {/* Summary Cards Section */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {/* Total de Entradas Card */}
        <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-500">Total de Entradas</p>
            {isLoading ? (
              <h2 className="text-2xl font-bold text-green-600">
                Carregando...
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
            <p className="text-xs text-gray-400">Receitas acumuladas</p>
          </div>
          <ArrowUp className="text-green-500" size={24} />
        </div>

        {/* Total de Saídas Card */}
        <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-500">Total de Saídas</p>
            {isLoading ? (
              <h2 className="text-2xl font-bold text-red-600">Carregando...</h2>
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
            <p className="text-xs text-gray-400">Despesas acumuladas</p>
          </div>
          <ArrowDown className="text-red-500" size={24} />
        </div>

        {/* Saldo Total Card */}
        <div className="bg-white rounded-lg shadow-sm p-6 flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-500">Saldo Total</p>
            {isLoading ? (
              <h2 className="text-2xl font-bold text-gray-800">
                Carregando...
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
            <p className="text-xs text-gray-400">Saldo atual</p>
          </div>
          <Calendar className="text-gray-500" size={24} />
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
            <button className="bg-green-600 text-white px-4 py-2 rounded-lg flex items-center">
              <Plus size={18} className="mr-2" />
              Novo Lançamento
            </button>
            <button
              className="border border-gray-300 text-gray-700 px-4 py-2 rounded-lg flex items-center"
              onClick={handleExport}
              disabled={isLoading}
            >
              {isLoading ? (
                "Exportando..."
              ) : (
                <>
                  <Download size={18} className="mr-2" />
                  Exportar
                </>
              )}
            </button>
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
            <p>Carregando lançamentos...</p>
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
                      <button className="text-gray-700 hover:text-gray-900">
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
    </main>
  );
}
