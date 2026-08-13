"use client";

import { useState } from "react";
import {
  type TransactionRequest,
  type TransactionResponse,
  transactionService,
} from "@/services/transactionService";
import { getErrorMessage } from "@/types/errorType";
import type { Page } from "@/types/PagesType";

export function useTransaction() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const getAllTransactions = async (
    search?: string,
    type?: string,
    category?: string,
    page: number = 0,
    size: number = 20,
  ): Promise<Page<TransactionResponse> | undefined> => {
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
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

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
    } catch (err) {
      setError(getErrorMessage(err));
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
    } catch (err) {
      setError(getErrorMessage(err));
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
    } catch (err) {
      setError(getErrorMessage(err));
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
    } catch (err) {
      setError(getErrorMessage(err));
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
    } catch (err) {
      setError(getErrorMessage(err));
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
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const exportTransactionsAsExcel = async (
    startDate?: string,
    endDate?: string,
  ): Promise<Blob | undefined> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionService.exportTransactionsAsExcel(
        startDate,
        endDate,
      );
      const url = window.URL.createObjectURL(new Blob([data]));
      const link = document.createElement("a");
      link.href = url;
      const fileName =
        startDate && endDate
          ? `HORTIFRUTI_SANTA_LUZIA_${startDate}_a_${endDate}.xlsx`
          : (() => {
              const { month, year } = getPreviousMonth();
              return `HORTIFRUTI_SANTA_LUZIA_${month}/${year}.xlsx`;
            })();
      link.setAttribute("download", fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      return data;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const exportTransactionsComplete = async (
    startDate?: string,
    endDate?: string,
  ): Promise<Blob | undefined> => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionService.exportTransactionsComplete(
        startDate,
        endDate,
      );
      const url = window.URL.createObjectURL(new Blob([data]));
      const link = document.createElement("a");
      link.href = url;
      const fileName =
        startDate && endDate
          ? `Relatorio-Hortifruti-Santa-Luzia-${startDate}_a_${endDate}.zip`
          : (() => {
              const { month, year } = getPreviousMonth();
              return `Relatorio-Hortifruti-Santa-Luzia-${month}-${year}.zip`;
            })();
      link.setAttribute("download", fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      return data;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const getPreviousMonth = () => {
    const now = new Date();
    let month = now.getMonth(); // 0-based, so January is 0
    let year = now.getFullYear();
    if (month === 0) {
      month = 12;
      year -= 1;
    }
    const monthStr = month.toString().padStart(2, "0");
    return { month: monthStr, year };
  };

  return {
    isLoading,
    error,
    getTotalRevenue,
    getTotalExpenses,
    getTotalBalance,
    getAllTransactions,
    updateTransaction,
    deleteTransaction,
    exportTransactionsAsExcel,
    exportTransactionsComplete,
    getAllCategories,
  };
}
