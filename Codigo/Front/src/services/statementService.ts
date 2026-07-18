"use client";

import { getAuthHeadersForFormData } from "@/utils/httpUtils";
import { API_BASE_URL } from "@/config/api";

export interface StatementResponse {
  id: number;
  filename: string;
  uploadDate: string;
  status: string;
  message?: string;
}

export const statementService = {
  async uploadStatements(files: File[]): Promise<{ message: string }> {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append("files", file);
    });

    try {
      const response = await fetch(`${API_BASE_URL}/statements/import`, {
        method: "POST",
        headers: getAuthHeadersForFormData(),
        credentials: "include",
        body: formData,
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || "Erro ao enviar arquivos");
      }

      const data = await response.text();
      return { message: data };
    } catch (error) {
      console.error("Erro ao enviar arquivos:", error);
      throw error;
    }
  },

  async listStatements(): Promise<StatementResponse[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/statements`, {
        method: "GET",
        headers: getAuthHeadersForFormData(),
        credentials: "include",
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

  async deleteStatement(id: number): Promise<void> {
    try {
      const response = await fetch(`${API_BASE_URL}/statements/${id}`, {
        method: "DELETE",
        headers: getAuthHeadersForFormData(),
        credentials: "include",
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
