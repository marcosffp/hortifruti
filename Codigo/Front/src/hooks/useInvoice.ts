"use client";

import { useState } from "react";
import { invoiceService } from "@/services/invoiceService";
import type {
  InvoiceResponseGet,
  InvoiceWithBilletResult,
  OpenInvoiceResponse,
} from "@/types/invoiceType";

export function useInvoice() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const generateInvoice = async (
    combinedScoreId: number,
    dadosAdicionais?: string,
  ) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await invoiceService.generateInvoice(
        combinedScoreId,
        dadosAdicionais,
      );
      return response;
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Erro ao gerar nota fiscal",
      );
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const getInvoiceInfo = async (
    ref: string,
  ): Promise<InvoiceResponseGet | null> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await invoiceService.fetchInvoiceInfo(ref);
      return result;
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Erro ao buscar informações da nota fiscal",
      );
      return null;
    } finally {
      setIsLoading(false);
    }
  };

  const getDanfe = async (ref: string): Promise<Blob> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await invoiceService.downloadDanfe(ref);
      return result;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao baixar DANFE");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const getXml = async (ref: string): Promise<Blob> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await invoiceService.downloadXml(ref);
      return result;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao baixar XML");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const generateInvoiceWithBillet = async (
    combinedScoreId: number,
    dadosAdicionais?: string,
    dueDate?: string,
  ): Promise<InvoiceWithBilletResult> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await invoiceService.issueInvoiceAndBillet(
        combinedScoreId,
        dadosAdicionais,
        dueDate,
      );
      return result;
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Erro ao gerar nota fiscal e boleto",
      );
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const getOpenInvoiceOnly = async (): Promise<OpenInvoiceResponse[]> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await invoiceService.getOpenInvoiceOnly();
      return result;
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Erro ao buscar notas fiscais em aberto",
      );
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const cancelInvoice = async (
    ref: string,
    justificativa: string,
    extemporaneo?: boolean,
  ) => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await invoiceService.cancelInvoice(
        ref,
        justificativa,
        extemporaneo,
      );
      return result;
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Erro ao cancelar nota fiscal",
      );
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    generateInvoice,
    generateInvoiceWithBillet,
    getInvoiceInfo,
    getDanfe,
    getXml,
    getOpenInvoiceOnly,
    cancelInvoice,
    isLoading,
    error,
  };
}
