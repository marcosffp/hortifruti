import { useState } from "react";
import { reportService } from "@/services/reportService";

export function useReport() {
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const generateReport = async (startDate: string, endDate: string) => {
    setIsGenerating(true);
    setError(null);
    try {
      if (!startDate || !endDate || startDate === endDate)
        throw new Error("Informe um intervalo de datas válido");
      const start = startDate.split("T")[0];
      const end = endDate.split("T")[0];
      const result = await reportService.fetchMonthlyReport(start, end);
      downloadReport(result, `RELATORIO_FISCAL_${start}_A_${end}.zip`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erro ao gerar relatório");
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
