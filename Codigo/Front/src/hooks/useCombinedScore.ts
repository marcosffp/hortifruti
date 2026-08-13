"use client";

import { useCallback, useState } from "react";
import { combinedScoreService } from "@/services/combinedScoreService";
import type { CombinedScoreRequest } from "@/types/combinedScoreType";
import { getErrorMessage } from "@/types/errorType";

export function useCombinedScore() {
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

  const fetchGroupedProducts = useCallback(
    (combinedScoreId: number) =>
      run(() => combinedScoreService.fetchGroupedProducts(combinedScoreId)),
    [run],
  );

  const fetchImages = useCallback(
    (combinedScoreId: number) =>
      run(() => combinedScoreService.fetchImages(combinedScoreId)),
    [run],
  );

  const downloadPhotosPdf = useCallback(
    (combinedScoreId: number) =>
      run(() => combinedScoreService.downloadPhotosPdf(combinedScoreId)),
    [run],
  );

  const createCombinedScore = useCallback(
    (request: CombinedScoreRequest) =>
      run(() => combinedScoreService.createCombinedScore(request)),
    [run],
  );

  const cancelGrouping = useCallback(
    (id: number) => run(() => combinedScoreService.cancelGrouping(id)),
    [run],
  );

  const confirmPayment = useCallback(
    (id: number) => run(() => combinedScoreService.confirmPayment(id)),
    [run],
  );

  const cancelPayment = useCallback(
    (id: number) => run(() => combinedScoreService.cancelPayment(id)),
    [run],
  );

  const createWildcardBillet = useCallback(
    (clientId: number, value: number) =>
      run(() => combinedScoreService.createWildcardBillet(clientId, value)),
    [run],
  );

  return {
    fetchGroupedProducts,
    fetchImages,
    downloadPhotosPdf,
    createCombinedScore,
    cancelGrouping,
    confirmPayment,
    cancelPayment,
    createWildcardBillet,
    isLoading,
    error,
  };
}
