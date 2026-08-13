"use client";

import { useCallback, useState } from "react";
import { clientService } from "@/services/clientService";
import { getErrorMessage } from "@/types/errorType";

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
      return await clientService.getClientById(clientId);
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { getClientById, isLoading, error };
}
