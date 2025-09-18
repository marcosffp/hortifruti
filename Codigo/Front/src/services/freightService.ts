"use client";

import { Geolocation } from "@/types/addressType";
import { getAuthHeaders } from "@/utils/httpUtils";

export interface FreightRequest {
  origin: Geolocation;
  destination: Geolocation;
}

export interface FreightResponse {
  distance: string;
  duration: string;
  freight: number;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const freightService = {
  async calculateFreight(
    origin: Geolocation,
    destination: Geolocation,
  ): Promise<FreightResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/distance`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({ origin, destination }),
      });

      if (!response.ok) {
        throw new Error(`Erro ao calcular frete: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error("Falha ao calcular frete:", error);
      throw error;
    }
  },
};