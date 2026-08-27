"use client";

import { useCallback, useState } from "react";
import {
  type ConfirmarCapturaRequest,
  capturaNotaService,
} from "@/services/capturaNotaService";
import { getErrorMessage } from "@/types/errorType";

export function useCapturaNota() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = useCallback(async <T>(fn: () => Promise<T>): Promise<T> => {
    setIsLoading(true);
    setError(null);
    try {
      return await fn();
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fetchPendentes = useCallback(
    () => run(() => capturaNotaService.fetchPendentes()),
    [run],
  );

  const fetchImagem = useCallback(
    (capturaId: number) => run(() => capturaNotaService.fetchImagem(capturaId)),
    [run],
  );

  const descartar = useCallback(
    (capturaId: number) => run(() => capturaNotaService.descartar(capturaId)),
    [run],
  );

  const reprocessar = useCallback(
    (capturaId: number) => run(() => capturaNotaService.reprocessar(capturaId)),
    [run],
  );

  const confirmar = useCallback(
    (capturaId: number, payload: ConfirmarCapturaRequest) =>
      run(() => capturaNotaService.confirmar(capturaId, payload)),
    [run],
  );

  return {
    fetchPendentes,
    fetchImagem,
    descartar,
    reprocessar,
    confirmar,
    isLoading,
    error,
  };
}
