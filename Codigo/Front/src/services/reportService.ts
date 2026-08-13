import { API_BASE_URL } from "@/config/api";
import { getAuthHeaders } from "@/utils/httpUtils";

export const reportService = {
  async fetchMonthlyReport(startDate: string, endDate: string): Promise<Blob> {
    const response = await fetch(
      `${API_BASE_URL}/dashboard/icms-report/monthly/${startDate}/${endDate}`,
      { headers: getAuthHeaders(), credentials: "include" },
    );
    if (!response.ok) {
      throw new Error("Failed to fetch monthly report");
    }
    return await response.blob();
  },
};
