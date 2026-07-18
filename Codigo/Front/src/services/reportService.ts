import { getAuthHeaders } from "@/utils/httpUtils";
import { API_BASE_URL } from "@/config/api";

export const reportService = {
  async fetchMonthlyReport(startDate: string, endDate: string): Promise<Blob> {
    const sDate = startDate;
    const eDate = endDate;
    const response = await fetch(
      `${API_BASE_URL}/icms-report/monthly/${sDate}/${eDate}`,
      { headers: getAuthHeaders(), credentials: "include" }
    );
    if (!response.ok) {
      throw new Error("Failed to fetch monthly report");
    }
    return await response.blob();
  }
};
