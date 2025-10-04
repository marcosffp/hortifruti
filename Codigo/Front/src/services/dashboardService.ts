import { getAuthHeaders } from "@/utils/httpUtils";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export interface DashboardData {
  Totais: {
    TotalReceita: number;
    TotalCusto: number;
    MargemLucro: number;
  };
  ReceitasPorTipo: {
    VendasCartao: number;
    VendasPix: number;
  };
  FluxoDeCaixa: {
    [month: string]: {
      Receitas?: number;
      Despesas?: number;
    };
  };
  PorcentagemPorCategoria: {
    [category: string]: {
      Porcentagem: number;
      Valor: number;
    };
  };
  RankingCategoriasGastos: Array<{
    Categoria: string;
    Valor: number;
    Rank: number;
  }>;
}

export const dashboardService = {
  async getDashboardData(
    startDate: string,
    endDate: string,
    month: number,
    year: number
  ): Promise<DashboardData> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/dashboard?startDate=${startDate}&endDate=${endDate}&month=${month}&year=${year}`,
        {
          method: "GET",
          headers: getAuthHeaders(),
        }
      );

      if (!response.ok) {
        throw new Error(`Erro ao buscar dados do dashboard: ${response.statusText}`);
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error("Erro ao buscar dados do dashboard:", error);
      throw error;
    }
  },
};