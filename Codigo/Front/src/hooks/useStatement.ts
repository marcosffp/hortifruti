"use client";

import { useState } from "react";
import { statementService } from "@/services/statementService";

export function useStatement() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validateFiles = (selectedFiles: File[]): File[] => {
    setError(null);
    const validFiles = selectedFiles.filter((file) => {
      // Verificar o tamanho do arquivo (10MB = 10 * 1024 * 1024 bytes)
      if (file.size > 10 * 1024 * 1024) {
        setError(`O arquivo ${file.name} excede o limite de 10MB.`);
        return false;
      }

      // Verificar se é um PDF
      if (file.type !== "application/pdf") {
        setError(`O arquivo ${file.name} não é um PDF.`);
        return false;
      }

      return true;
    });

    return validFiles;
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + " B";
    else if (bytes < 1048576) return (bytes / 1024).toFixed(1) + " KB";
    else return (bytes / 1048576).toFixed(1) + " MB";
  };

  const processFiles = async (files: File[]) => {
    setIsLoading(true);
    setError(null);

    try {
      // Lógica de validação ou transformação de arquivos
      if (files.some((file) => file.size > 10 * 1024 * 1024)) {
        throw new Error("Um ou mais arquivos excedem o limite de 10MB.");
      }

      // Chamada ao serviço para upload
      const response = await statementService.uploadStatements(files);
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