"use client";

import { useState } from "react";
import { statementService } from "@/services/statementService";

export function useStatement() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validateFiles = (selectedFiles: File[]): File[] => {
    setError(null);
    const validFiles = selectedFiles.filter((file) => {
      if (file.size > 10 * 1024 * 1024) {
        setError(`O arquivo ${file.name} excede o limite de 10MB.`);
        return false;
      }

      if (file.type !== "application/pdf") {
        setError(`O arquivo ${file.name} não é um PDF.`);
        return false;
      }

      return true;
    });

    return validFiles;
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    else if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    else return `${(bytes / 1048576).toFixed(1)} MB`;
  };

  const processFiles = async (files: File[]) => {
    setIsLoading(true);
    setError(null);

    try {
      if (files.some((file) => file.size > 10 * 1024 * 1024)) {
        throw new Error("Um ou mais arquivos excedem o limite de 10MB.");
      }

      const response = await statementService.uploadStatements(files);
      return response;
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Erro ao processar os arquivos.",
      );
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
