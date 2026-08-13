import { API_BASE_URL } from "@/config/api";
import type { FiscalNoteXmlStorageResponse } from "@/types/fiscalNoteXmlStorageType";
import { getAuthHeaders } from "@/utils/httpUtils";

export const fiscalNoteXmlStorageService = {
  async getByPeriod(
    startDate: string,
    endDate: string,
  ): Promise<FiscalNoteXmlStorageResponse[]> {
    const params = new URLSearchParams({ startDate, endDate });
    const response = await fetch(
      `${API_BASE_URL}/invoices/xml-storage?${params.toString()}`,
      { method: "GET", headers: getAuthHeaders(), credentials: "include" },
    );
    if (!response.ok) {
      throw new Error(`Erro ao buscar XMLs: ${response.status}`);
    }
    return response.json();
  },

  async downloadXml(ref: string, nfNumber: string): Promise<void> {
    const response = await fetch(
      `${API_BASE_URL}/invoices/xml-storage/${encodeURIComponent(ref)}/download`,
      { method: "GET", headers: getAuthHeaders(), credentials: "include" },
    );
    if (!response.ok) {
      throw new Error(`Erro ao baixar XML: ${response.status}`);
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${nfNumber}.xml`;
    a.click();
    URL.revokeObjectURL(url);
  },
};
