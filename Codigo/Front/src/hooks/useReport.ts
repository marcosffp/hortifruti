import { reportService } from "@/services/reportService";
import { useState } from "react";

export function useReport() {
    const [isGenerating, setIsGenerating] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const generateReport = async (startDate: string, endDate: string) => {
        setIsGenerating(true);
        setError(null);
        try {
            if(!startDate || !endDate || startDate === endDate)
                throw new Error("Informe um intervalo de datas válido");
            startDate = startDate.split('T')[0];
            endDate = endDate.split('T')[0];
            const result = await reportService.fetchMonthlyReport(startDate, endDate);
            downloadReport(result, `RELATORIO_FISCAL_${startDate}_A_${endDate}.zip`);
        } catch (e: any) {
            setError(e.message || "Erro ao gerar relatório");
        } finally {
            setIsGenerating(false);
        }
    };

    const downloadReport = (blob: Blob, filename: string) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = filename;
        a.click();
        window.URL.revokeObjectURL(url);
    };

    return {
        isGenerating,
        error,
        generateReport,
    };
}