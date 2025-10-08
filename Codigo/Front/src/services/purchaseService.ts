"use client";

import { getAuthHeaders, getAuthHeadersForFormData } from "@/utils/httpUtils";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const purchaseService = {
  // Upload de notas fiscais
  async uploadPurchases(files: File[]): Promise<{ message: string }> {
    const formData = new FormData();
    formData.append("file", files[0]);

    try {
      const response = await fetch(`${API_BASE_URL}/purchases/process`, {
        method: "POST",
        headers: getAuthHeadersForFormData(),
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

  // Listar notas fiscais
  async listPurchases(): Promise<any[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/purchases`, {
        method: "GET",
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || "Erro ao listar notas fiscais");
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Erro ao listar notas fiscais:", error);
      throw error;
    }
  },
};
