"use client";

import { useCallback, useEffect, useState } from "react";
import {
  type BankBalance,
  bankBalanceService,
} from "@/services/bankBalanceService";

export function useBankBalance() {
  const [balance, setBalance] = useState<BankBalance | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchBalance = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await bankBalanceService.getSaldoDisponivel();
      setBalance(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Erro ao buscar saldo bancário.",
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBalance();
  }, [fetchBalance]);

  return { balance, isLoading, error, refetch: fetchBalance };
}
