"use client";

import { useState } from "react";
import { invoiceService } from "@/services/invoiceService";
import { InvoiceResponse, InvoiceResponseGet } from "@/types/invoiceType";

export function useInvoice() {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const generateInvoice = async (combinedScoreId: number) => {
        setIsLoading(true);
        setError(null);
        try {
            const response = await invoiceService.generateInvoice(combinedScoreId);
            return response;
        } catch (err: any) {
            setError(err.message || "Erro ao gerar nota fiscal");
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    const getInvoiceInfo = async (ref: string): Promise<InvoiceResponseGet | null> => {
        setIsLoading(true);
        setError(null);
        try {
            const result = await invoiceService.fetchInvoiceInfo(ref);
            return result;
        } catch (err: any) {
            setError(err.message || "Erro ao buscar informações da nota fiscal");
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
        } catch (err: any) {
            setError(err.message || "Erro ao baixar DANFE");
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
        } catch (err: any) {
            setError(err.message || "Erro ao baixar XML");
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    const cancelInvoice = async (ref: string, justificativa: string) => {
        setIsLoading(true);
        setError(null);
        try {
            const result = await invoiceService.cancelInvoice(ref, justificativa);
            return result;
        } catch (err: any) {
            setError(err.message || "Erro ao cancelar nota fiscal");
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { 
        generateInvoice, 
        getInvoiceInfo, 
        getDanfe, 
        getXml,
        cancelInvoice, 
        isLoading, 
        error 
    };
}
