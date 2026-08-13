"use client";

import { useCallback, useEffect, useState } from "react";
import {
  type BankBalance,
  bankBalanceService,
} from "@/services/bankBalanceService";
import { getErrorMessage } from "@/types/errorType";

export function useBankBalance() {
  const [balance, setBalance] = useState<BankBalance | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchBalance = useCallback(async (signal?: AbortSignal) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await bankBalanceService.getSaldoDisponivel(signal);
      setBalance(data);
    } catch (err) {
      if (err instanceof DOMException && err.name === "AbortError") {
        return;
      }
      setError(getErrorMessage(err));
    } finally {
      if (!signal?.aborted) {
        setIsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    // Cancela a requisição se o usuário navegar para outra tela antes dela
    // terminar, liberando a conexão em vez de deixá-la disputando espaço com
    // a navegação (ver comentário em bankBalanceService sobre "priority: low").
    const controller = new AbortController();
    fetchBalance(controller.signal);
    return () => controller.abort();
  }, [fetchBalance]);

  return { balance, isLoading, error, refetch: fetchBalance };
}
