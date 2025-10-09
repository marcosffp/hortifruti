"use client";

import { fetchWithAuth } from "@/utils/httpUtils";

// Interface para Recomendação de Produtos
export interface ProductRecommendation {
  productId: number;
  name: string;
  temperatureCategory: 'CONGELANDO' | 'FRIO' | 'AMENO' | 'QUENTE';
  score: number;
  tag: 'BOM' | 'MEDIO' | 'RUIM';
}

// Interface para criação de produto
export interface ProductRequest {
  name: string;
  temperatureCategory: 'CONGELANDO' | 'FRIO' | 'AMENO' | 'QUENTE';
  peakSalesMonths: number[];
  lowSalesMonths: number[];
}

// Interface para resposta de produto
export interface ProductResponse {
  id: number;
  name: string;
  temperatureCategory: 'CONGELANDO' | 'FRIO' | 'AMENO' | 'QUENTE';
  peakSalesMonths: number[];
  lowSalesMonths: number[];
}

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Interface para previsão do tempo
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
  // Buscar previsão do tempo para 5 dias
  async getWeatherForecast(): Promise<WeatherForecast> {
    console.log('Buscando previsão do tempo em:', `${API_URL}/api/weather/forecast/5days`);
    const response = await fetchWithAuth(
      `${API_URL}/api/weather/forecast/5days`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    if (!response.ok) {
      console.error('Erro na resposta:', response.status, response.statusText);
      throw new Error(`Erro ao buscar previsão do tempo: ${response.statusText}`);
    }

    const data = await response.json();
    console.log('Dados de previsão recebidos:', data);
    return data;
  },

  // Buscar recomendações por data
  async getRecommendationsByDate(date: string): Promise<ProductRecommendation[]> {
    console.log('Buscando recomendações em:', `${API_URL}/api/recommendations/by-date?date=${date}`);
    const response = await fetchWithAuth(
      `${API_URL}/api/recommendations/by-date?date=${date}`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    if (!response.ok) {
      console.error('Erro na resposta:', response.status, response.statusText);
      throw new Error(`Erro ao buscar recomendações: ${response.statusText}`);
    }

    const data = await response.json();
    console.log('Dados de recomendações recebidos:', data);
    return data;
  },

  // Buscar recomendações por categoria de temperatura
  async getRecommendationsByTemperature(category: string): Promise<ProductRecommendation[]> {
    const response = await fetchWithAuth(
      `${API_URL}/api/recommendations/by-temperature/${category}`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    if (!response.ok) {
      throw new Error(`Erro ao buscar recomendações: ${response.statusText}`);
    }

    return response.json();
  },

  // Criar novo produto
  async createProduct(product: ProductRequest): Promise<ProductResponse> {
    const response = await fetchWithAuth(`${API_URL}/products`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(product),
    });

    if (!response.ok) {
      throw new Error(`Erro ao criar produto: ${response.statusText}`);
    }

    return response.json();
  },

  // Listar todos os produtos
  async getAllProducts(): Promise<ProductResponse[]> {
    const response = await fetchWithAuth(`${API_URL}/products`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Erro ao listar produtos: ${response.statusText}`);
    }

    return response.json();
  },

  // Atualizar produto existente
  async updateProduct(id: number, product: ProductRequest): Promise<ProductResponse> {
    const response = await fetchWithAuth(`${API_URL}/products/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(product),
    });

    if (!response.ok) {
      throw new Error(`Erro ao atualizar produto: ${response.statusText}`);
    }

    return response.json();
  },

  // Excluir produto
  async deleteProduct(id: number): Promise<void> {
    const response = await fetchWithAuth(`${API_URL}/products/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error(`Erro ao excluir produto: ${response.statusText}`);
    }
  },
};