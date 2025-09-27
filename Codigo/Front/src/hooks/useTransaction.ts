"use client";

import { useState } from "react";
import {
  transactionService,
  TransactionRequest,
  TransactionResponse,
  PageResult,
} from "@/services/transactionService";

export function useTransaction() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Novo método para buscar transações paginadas e filtradas
  const getAllTransactions = async (
    search?: string,
    type?: string,
    category?: string,
    page: number = 0,
    size: number = 20,
  ): Promise<PageResult<TransactionResponse> | undefined> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionService.getAllTransactions(
        search,
        type,
        category,
        page,
        size,
      );
      return data;
    } catch (err: any) {
      setError(err.message || "Erro ao buscar transações.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  // Atualizar os métodos para aceitar parâmetros de data
  const getTotalRevenue = async (
    startDate?: string,
    endDate?: string,
  ): Promise<number | undefined> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionService.getTotalRevenueForCurrentMonth(
        startDate,
        endDate,
      );
      return data;
    } catch (err: any) {
      setError(err.message || "Erro ao buscar receita total.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const getTotalExpenses = async (
    startDate?: string,
    endDate?: string,
  ): Promise<number | undefined> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionService.getTotalExpensesForCurrentMonth(
        startDate,
        endDate,
      );
      return data;
    } catch (err: any) {
      setError(err.message || "Erro ao buscar despesas totais.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const getTotalBalance = async (
    startDate?: string,
    endDate?: string,
  ): Promise<number | undefined> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionService.getTotalBalanceForCurrentMonth(
        startDate,
        endDate,
      );
      return data;
    } catch (err: any) {
      setError(err.message || "Erro ao buscar saldo total.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const getAllCategories = async (): Promise<string[]> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionService.getAllCategories();
      return data;
    } catch (err: any) {
      setError(err.message || "Erro ao buscar categorias.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const updateTransaction = async (
    id: number,
    transaction: TransactionRequest,
  ): Promise<TransactionResponse | undefined> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionService.updateTransaction(id, transaction);
      return data;
    } catch (err: any) {
      setError(err.message || "Erro ao atualizar transação.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const deleteTransaction = async (id: number): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      await transactionService.deleteTransaction(id);
    } catch (err: any) {
      setError(err.message || "Erro ao deletar transação.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const exportTransactionsAsExcel = async (): Promise<Blob | undefined> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionService.exportTransactionsAsExcel();
      const url = window.URL.createObjectURL(new Blob([data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "lancamentos.xlsx");
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      return data;
    } catch (err: any) {
      setError(err.message || "Erro ao exportar transações.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    isLoading,
    error,
    getTotalRevenue,
    getTotalExpenses,
    getTotalBalance,
    getAllTransactions, // agora suporta paginação e filtros
    updateTransaction,
    deleteTransaction,
    exportTransactionsAsExcel,
    getAllCategories,
  };
}
