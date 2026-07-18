"use client";

import { API_BASE_URL as API_URL } from "@/config/api";
import { fetchWithAuth } from "@/utils/httpUtils";

export type TemperatureCategory = "CONGELANDO" | "FRIO" | "AMENO" | "QUENTE";

export interface ProductRecommendation {
  productId: number;
  name: string;
  temperatureCategory: TemperatureCategory;
  score: number;
  tag: "BOM" | "MEDIO" | "RUIM";
}

export interface ProductRequest {
  name: string;
  temperatureCategory: TemperatureCategory;
  peakSalesMonths: number[];
  lowSalesMonths: number[];
}

export interface ProductResponse {
  id: number;
  name: string;
  temperatureCategory: TemperatureCategory;
  peakSalesMonths: number[];
  lowSalesMonths: number[];
}

export interface WeatherForecast {
  city: string;
  country: string;
  dailyForecasts: {
    date: string;
    minTemp: number;
    maxTemp: number;
    avgTemp: number;
    avgFeelsLike: number;
    humidity: number;
    rainfall: number;
    windSpeed: number;
    weatherDescription: string;
    weatherIcon: string;
  }[];
}

export const productService = {
  async getWeatherForecast(): Promise<WeatherForecast> {
    const response = await fetchWithAuth(
      `${API_URL}/api/weather/forecast/5days`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      },
    );

    if (!response.ok) {
      console.error("Erro na resposta:", response.status, response.statusText);
      throw new Error(
        `Erro ao buscar previsão do tempo: ${response.statusText}`,
      );
    }

    const data = await response.json();
    return data;
  },

  async getRecommendationsByDate(
    date: string,
  ): Promise<ProductRecommendation[]> {
    const response = await fetchWithAuth(
      `${API_URL}/api/recommendations/by-date?date=${date}`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      },
    );

    if (!response.ok) {
      console.error("Erro na resposta:", response.status, response.statusText);
      throw new Error(`Erro ao buscar recomendações: ${response.statusText}`);
    }

    const data = await response.json();
    return data;
  },

  async getRecommendationsByTemperature(
    category: string,
  ): Promise<ProductRecommendation[]> {
    const response = await fetchWithAuth(
      `${API_URL}/api/recommendations/by-temperature/${category}`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      },
    );

    if (!response.ok) {
      throw new Error(`Erro ao buscar recomendações: ${response.statusText}`);
    }

    return response.json();
  },

  async createProduct(product: ProductRequest): Promise<ProductResponse> {
    const response = await fetchWithAuth(`${API_URL}/products`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(product),
    });

    if (!response.ok) {
      throw new Error(`Erro ao criar produto: ${response.statusText}`);
    }

    return response.json();
  },

  async getAllProducts(): Promise<ProductResponse[]> {
    const response = await fetchWithAuth(`${API_URL}/products`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error(`Erro ao listar produtos: ${response.statusText}`);
    }

    return response.json();
  },

  async updateProduct(
    id: number,
    product: ProductRequest,
  ): Promise<ProductResponse> {
    const response = await fetchWithAuth(`${API_URL}/products/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(product),
    });

    if (!response.ok) {
      throw new Error(`Erro ao atualizar produto: ${response.statusText}`);
    }

    return response.json();
  },

  async deleteProduct(id: number): Promise<void> {
    const response = await fetchWithAuth(`${API_URL}/products/${id}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      throw new Error(`Erro ao excluir produto: ${response.statusText}`);
    }
  },
};
