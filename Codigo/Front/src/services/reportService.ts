import { getAuthHeaders } from "@/utils/httpUtils";

const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

export const reportService = {
  async fetchMonthlyReport(startDate: string, endDate: string): Promise<Blob> {
    const sDate = startDate;
    const eDate = endDate;
    const response = await fetch(
      `${API_BASE_URL}/icms-report/monthly/${sDate}/${eDate}`,
      { headers: getAuthHeaders() }
    );
    if (!response.ok) {
      throw new Error("Failed to fetch monthly report");
    }
    return await response.blob();
  }
};