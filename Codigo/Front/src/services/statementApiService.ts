"use client";

import { API_BASE_URL } from "@/config/api";
import { getAuthHeaders } from "@/utils/httpUtils";

export interface SicoobImportSummary {
  statementId: number;
  alreadyProcessed: boolean;
  periodStart: string;
  periodEnd: string;
  totalFetched: number;
  totalSaved: number;
  totalDuplicatedSkipped: number;
  totalEntradas: number;
  totalSaidas: number;
}

export interface BBImportSummary {
  statementId: number;
  alreadyProcessed: boolean;
  periodStart: string;
  periodEnd: string;
  totalFetched: number;
  totalSaved: number;
  totalDuplicatedSkipped: number;
  totalEntradas: number;
  totalSaidas: number;
}

async function parseErrorMessage(
  response: Response,
  fallback: string,
): Promise<string> {
  try {
    const data = await response.json();
    if (data?.message) return data.message;
  } catch {
    // corpo não é JSON, usa a mensagem padrão
  }
  return fallback;
}

export const statementApiService = {
  async importSicoob(
    mes: number,
    ano: number,
    diaInicial?: number,
    diaFinal?: number,
  ): Promise<SicoobImportSummary> {
    const params = new URLSearchParams({ mes: String(mes), ano: String(ano) });
    if (diaInicial != null) params.append("diaInicial", String(diaInicial));
    if (diaFinal != null) params.append("diaFinal", String(diaFinal));

    const response = await fetch(
      `${API_BASE_URL}/statements/sicoob-api/import?${params.toString()}`,
      {
        method: "POST",
        headers: getAuthHeaders(),
        credentials: "include",
      },
    );

    if (!response.ok) {
      throw new Error(
        await parseErrorMessage(response, "Erro ao buscar extrato do Sicoob"),
      );
    }

    return response.json();
  },

  async importBB(
    dataInicio: string,
    dataFim: string,
  ): Promise<BBImportSummary> {
    const params = new URLSearchParams({ dataInicio, dataFim });

    const response = await fetch(
      `${API_BASE_URL}/statements/bb-api/import?${params.toString()}`,
      {
        method: "POST",
        headers: getAuthHeaders(),
        credentials: "include",
      },
    );

    if (!response.ok) {
      throw new Error(
        await parseErrorMessage(response, "Erro ao buscar extrato do BB"),
      );
    }

    return response.json();
  },

  async downloadSicoobPdf(
    mes: number,
    ano: number,
    diaInicial?: number,
    diaFinal?: number,
  ): Promise<Blob> {
    const params = new URLSearchParams({ mes: String(mes), ano: String(ano) });
    if (diaInicial != null) params.append("diaInicial", String(diaInicial));
    if (diaFinal != null) params.append("diaFinal", String(diaFinal));

    const response = await fetch(
      `${API_BASE_URL}/statements/sicoob-api/export/pdf?${params.toString()}`,
      { headers: getAuthHeaders(), credentials: "include" },
    );

    if (!response.ok) {
      throw new Error(
        await parseErrorMessage(response, "Erro ao baixar PDF do Sicoob"),
      );
    }

    return response.blob();
  },

  async downloadSicoobExcel(
    mes: number,
    ano: number,
    diaInicial?: number,
    diaFinal?: number,
  ): Promise<Blob> {
    const params = new URLSearchParams({ mes: String(mes), ano: String(ano) });
    if (diaInicial != null) params.append("diaInicial", String(diaInicial));
    if (diaFinal != null) params.append("diaFinal", String(diaFinal));

    const response = await fetch(
      `${API_BASE_URL}/statements/sicoob-api/export/excel?${params.toString()}`,
      { headers: getAuthHeaders(), credentials: "include" },
    );

    if (!response.ok) {
      throw new Error(
        await parseErrorMessage(response, "Erro ao baixar Excel do Sicoob"),
      );
    }

    return response.blob();
  },

  async downloadBBPdf(dataInicio: string, dataFim: string): Promise<Blob> {
    const params = new URLSearchParams({ dataInicio, dataFim });

    const response = await fetch(
      `${API_BASE_URL}/statements/bb-api/export/pdf?${params.toString()}`,
      { headers: getAuthHeaders(), credentials: "include" },
    );

    if (!response.ok) {
      throw new Error(
        await parseErrorMessage(response, "Erro ao baixar PDF do BB"),
      );
    }

    return response.blob();
  },

  async downloadBBExcel(dataInicio: string, dataFim: string): Promise<Blob> {
    const params = new URLSearchParams({ dataInicio, dataFim });

    const response = await fetch(
      `${API_BASE_URL}/statements/bb-api/export/excel?${params.toString()}`,
      { headers: getAuthHeaders(), credentials: "include" },
    );

    if (!response.ok) {
      throw new Error(
        await parseErrorMessage(response, "Erro ao baixar Excel do BB"),
      );
    }

    return response.blob();
  },
};
