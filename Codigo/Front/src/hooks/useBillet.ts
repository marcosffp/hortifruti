"use client";

import { useState } from "react";
import { billetService } from "@/services/billetService";
import { BilletFilters, BilletResponse, OpenBilletResponse } from "@/types/billetType";

export function useBillet() {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const downloadBillet = async (blob: Blob, combinedScoreId: number, number: string) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `BOL-${number}.pdf`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
    };

    const generateBillet = async (combinedScoreId: number, number: string, dueDate?: string) => {
        setIsLoading(true);
        setError(null);
        try {
            const blob = await billetService.generateBillet(combinedScoreId, number, dueDate);
            downloadBillet(blob, combinedScoreId, number);
            return blob;
        } catch (err: any) {
            setError(err.message || "Erro ao gerar boleto");
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    const getBilletInfo = async (combinedScoreId: number): Promise<BilletResponse | null> => {
        setIsLoading(true);
        setError(null);
        try {
            const result = await billetService.fetchBilletInfo(combinedScoreId);
            return result;
        } catch (err: any) {
            setError(err.message || "Erro ao buscar informações do boleto");
            return null;
        } finally {
            setIsLoading(false);
        }
    };

    const getClientBillets = async (clientId: number, filters?: BilletFilters) => {
        setIsLoading(true);
        setError(null);
        try {
            const result = await billetService.getClientBillets(clientId, filters);
            return result;
        } catch (err: any) {
            setError(err.message || "Erro ao buscar boletos do cliente");
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
        } catch (err: any) {
            setError(err.message || "Erro ao buscar boletos em aberto");
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
        } catch (err: any) {
            setError(err.message || "Erro ao emitir 2ª via do boleto");
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
        } catch (err: any) {
            setError(err.message || "Erro ao cancelar boleto");
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
        } catch (err: any) {
            setError(err.message || "Erro ao confirmar pagamento do boleto");
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
        cancelBillet,
        markBilletAsPaid,
        isLoading,
        error
    };
}