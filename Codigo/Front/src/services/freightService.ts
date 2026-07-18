"use client";

import { API_BASE_URL } from "@/config/api";
import type { Geolocation } from "@/types/addressType";
import type { FreightConfigDTO } from "@/types/freightType";
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

export const freightService = {
  async calculateFreight(
    origin: Geolocation,
    destination: Geolocation,
  ): Promise<FreightResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/distance`, {
        method: "POST",
        headers: getAuthHeaders(),
        credentials: "include",
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

  async getFreightConfig(): Promise<FreightConfigDTO> {
    try {
      const response = await fetch(`${API_BASE_URL}/distance/freight-config`, {
        method: "GET",
        headers: getAuthHeaders(),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(
          `Erro ao buscar configurações de frete: ${response.status}`,
        );
      }

      return response.json();
    } catch (error) {
      console.error("Falha ao buscar configurações de frete:", error);
      throw error;
    }
  },

  async updateFreightConfig(
    config: Partial<FreightConfigDTO>,
  ): Promise<FreightConfigDTO> {
    try {
      const response = await fetch(`${API_BASE_URL}/distance/freight-config`, {
        method: "PATCH",
        headers: getAuthHeaders(),
        body: JSON.stringify(config),
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(
          `Erro ao atualizar configurações de frete: ${response.status}`,
        );
      }

      return await response.json();
    } catch (error) {
      console.error("Falha ao atualizar configurações de frete:", error);
      throw error;
    }
  },
};
