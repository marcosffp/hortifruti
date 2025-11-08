"use client";

import { getAuthHeadersForFormData } from "@/utils/httpUtils";

export interface BackupResponseDTO {
  message: string;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

function buildQuery(params: Record<string, string | undefined>) {
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v) usp.append(k, v);
  });
  const s = usp.toString();
  return s ? `?${s}` : "";
}

export const backupService = {
  async getStorage(): Promise<{ current: number; max: number; raw: string; percentage: number }> {
    const response = await fetch(`${API_BASE_URL}/backup/storage`, {
      method: "GET",
      headers: getAuthHeadersForFormData(),
    });

    if (!response.ok) {
      const errorData = await response.text();
      throw new Error(errorData || "Erro ao obter armazenamento do banco");
    }

    const data: BackupResponseDTO = await response.json();
    const raw = data.message || "";

    // Formato esperado: "123.45/5120 MB"
    const [currentStr, rest] = raw.split("/");
    const maxStr = (rest || "").replace("MB", "").trim();

    const current = parseFloat((currentStr || "0").trim());
    const max = parseFloat(maxStr || "0");
    const percentage = max > 0 ? Math.min(100, Math.round((current / max) * 100)) : 0;

    return { current, max, raw, percentage };
  },

  async startBackup(
    startDate?: string,
    endDate?: string
  ): Promise<{ message: string; requiresAuth: boolean; authUrl?: string }> {
    const query = buildQuery({ startDate, endDate });

    const response = await fetch(`${API_BASE_URL}/backup${query}`, {
      method: "POST",
      headers: getAuthHeadersForFormData(),
    });

    if (!response.ok) {
      const errorData = await response.text();
      throw new Error(errorData || "Erro ao iniciar backup");
    }

    const data: BackupResponseDTO = await response.json();
    const msg = data.message || "";
    const isUrl = /^https?:\/\//i.test(msg);

    return { message: msg, requiresAuth: isUrl, authUrl: isUrl ? msg : undefined };
  },
};