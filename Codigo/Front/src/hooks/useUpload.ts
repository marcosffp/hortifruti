"use client";

import { useState } from "react";
import { statementService } from "@/services/statementService";
import { purchaseService } from "@/services/purchaseService";
import { validarArquivos } from "@/utils/validationUtils";

export function useUpload() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validateFiles = (selectedFiles: File[]): File[] => {
    setError(null);
    const result = validarArquivos(selectedFiles);
    if (typeof result === "string") {
      setError(result);
      return [];
    }
    return result;
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + " B";
    else if (bytes < 1048576) return (bytes / 1024).toFixed(1) + " KB";
    else return (bytes / 1048576).toFixed(1) + " MB";
  };

  const processFiles = async (files: File[], entity: string) => {
    setIsLoading(true);
    setError(null);

    try {
      // Lógica de validação ou transformação de arquivos
      if (files.some((file) => file.size > 10 * 1024 * 1024)) {
        throw new Error("Um ou mais arquivos excedem o limite de 10MB.");
      }

      let response;
      if (entity === "purchase")
        response = await purchaseService.uploadPurchases(files);
      else if (entity === "statement")
        response = await statementService.uploadStatements(files);
      else throw new Error("Entidade desconhecida.");

      return response;
    } catch (err: any) {
      setError(err.message || "Erro ao processar os arquivos.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  return {
    isLoading,
    error,
    processFiles,
    formatFileSize,
    validateFiles,
  };
}
