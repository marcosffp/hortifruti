"use client";

import { getAuthHeadersForFormData } from "@/app/utils/httpUtils";

export interface StatementResponse {
  id: number;
  filename: string;
  uploadDate: string;
  status: string;
  message?: string;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const statementService = {
  // Upload de extratos
  async uploadStatements(files: File[]): Promise<StatementResponse[]> {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append("files", file);
    });

    try {
      const response = await fetch(`${API_BASE_URL}/statements/import`, {
        method: "POST",
        headers: getAuthHeadersForFormData(),
        body: formData,
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || "Erro ao enviar arquivos");
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Erro ao enviar arquivos:", error);
      throw error;
    }
  },

  // Listar extratos
  async listStatements(): Promise<StatementResponse[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/statements`, {
        method: "GET",
        headers: getAuthHeadersForFormData(),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || "Erro ao listar extratos");
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Erro ao listar extratos:", error);
      throw error;
    }
  },

  // Excluir extrato por ID
  async deleteStatement(id: number): Promise<void> {
    try {
      const response = await fetch(`${API_BASE_URL}/statements/${id}`, {
        method: "DELETE",
        headers: getAuthHeadersForFormData(),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || "Erro ao excluir extrato");
      }
    } catch (error) {
      console.error("Erro ao excluir extrato:", error);
      throw error;
    }
  },
};
