"use client";

import { useCallback, useState } from "react";
import { API_BASE_URL } from "@/config/api";
import { getAuthHeaders } from "@/utils/httpUtils";

export function useClient() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // useCallback é essencial aqui: componentes que usam getClientById como
  // dependência de useEffect (ex: CombinedScoresCards) entravam em loop infinito
  // de fetch, já que uma referência nova a cada render (causada pelos próprios
  // setIsLoading/setError abaixo) reexecutava o efeito indefinidamente.
  const getClientById = useCallback(async (clientId: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_BASE_URL}/clients/${clientId}`, {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Erro ao buscar cliente: ${response.status}`);
      }

      const result = await response.json();
      return result;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao buscar cliente");
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { getClientById, isLoading, error };
}
