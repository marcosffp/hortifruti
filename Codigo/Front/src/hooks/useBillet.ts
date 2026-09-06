"use client";

import { useState } from "react";
import { billetService } from "@/services/billetService";
import type {
  BilletFilters,
  BilletResponse,
  OpenBilletResponse,
} from "@/types/billetType";
import { getErrorMessage } from "@/types/errorType";
import { getFirstName, toFilenameSafe } from "@/utils/filenameUtils";

interface DownloadBilletOptions {
  clientName?: string | null;
  useStandardFileName?: boolean;
}

function buildBilletFilename(
  number: string,
  options?: DownloadBilletOptions,
): string {
  if (options?.useStandardFileName && options.clientName) {
    return `BOLETO_${toFilenameSafe(getFirstName(options.clientName))}.pdf`;
  }
  return `BOL-${number}.pdf`;
}

export function useBillet() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const downloadBillet = async (
    blob: Blob,
    _combinedScoreId: number,
    number: string,
    options?: DownloadBilletOptions,
  ) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", buildBilletFilename(number, options));
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const generateBillet = async (
    combinedScoreId: number,
    number: string,
    dueDate?: string,
    options?: DownloadBilletOptions,
  ) => {
    setIsLoading(true);
    setError(null);
    try {
      const blob = await billetService.generateBillet(
        combinedScoreId,
        number,
        dueDate,
      );
      downloadBillet(blob, combinedScoreId, number, options);
      return blob;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const getBilletInfo = async (
    combinedScoreId: number,
  ): Promise<BilletResponse | null> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await billetService.fetchBilletInfo(combinedScoreId);
      return result;
    } catch (err) {
      setError(getErrorMessage(err));
      return null;
    } finally {
      setIsLoading(false);
    }
  };

  const getClientBillets = async (
    clientId: number,
    filters?: BilletFilters,
  ) => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await billetService.getClientBillets(clientId, filters);
      return result;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const getOpenBillets = async (): Promise<OpenBilletResponse[]> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await billetService.getOpenBillets();
      return result;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const issueCopy = async (combinedScoreId: number): Promise<Blob> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await billetService.issueCopy(combinedScoreId);
      return result;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const downloadStoredBillet = async (
    combinedScoreId: number,
  ): Promise<Blob> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await billetService.downloadStoredBillet(combinedScoreId);
      return result;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const cancelBillet = async (combinedScoreId: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await billetService.cancelBillet(combinedScoreId);
      return result;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const cancelBilletByNumber = async (nossoNumero: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await billetService.cancelBilletByNumber(nossoNumero);
      return result;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const markBilletAsPaid = async (combinedScoreId: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await billetService.markBilletAsPaid(combinedScoreId);
      return result;
    } catch (err) {
      setError(getErrorMessage(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    downloadBillet,
    generateBillet,
    getBilletInfo,
    getClientBillets,
    getOpenBillets,
    issueCopy,
    downloadStoredBillet,
    cancelBillet,
    cancelBilletByNumber,
    markBilletAsPaid,
    isLoading,
    error,
  };
}
