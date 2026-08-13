import { API_BASE_URL } from "@/config/api";
import { getAuthHeaders } from "@/utils/httpUtils";

export interface TopProduct {
  Nome: string;
  QuantidadeTotal: number;
  ValorTotal: number;
}

export interface TopProductByQuantity {
  Nome: string;
  QuantidadeTotal: number;
}

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
    Rank: number;
    Valor: number;
  }>;
  "Fluxo de Vendas": {
    [week: string]: number;
  };
  "Produtos em Alta": TopProduct[];
  Top10ProdutosPorQuantidade: TopProductByQuantity[];
}

export interface GetDashboardDataParams {
  startDate: string;
  endDate: string;
  month: number;
  year: number;
  signal?: AbortSignal;
}

export const dashboardService = {
  async getDashboardData({
    startDate,
    endDate,
    month,
    year,
    signal,
  }: GetDashboardDataParams): Promise<DashboardData> {
    try {
      const params = new URLSearchParams({
        startDate,
        endDate,
        month: month.toString(),
        year: year.toString(),
      });
      const response = await fetch(
        `${API_BASE_URL}/dashboard?${params.toString()}`,
        {
          method: "GET",
          headers: getAuthHeaders(),
          credentials: "include",
          // Prioridade baixa: essa chamada é lenta (consulta APIs fiscais externas) e não pode
          // disputar conexão com a navegação do usuário para outras telas.
          priority: "low",
          signal,
        },
      );

      if (!response.ok) {
        throw new Error(
          `Erro ao buscar dados do dashboard: ${response.statusText}`,
        );
      }

      const data = await response.json();
      return data;
    } catch (error) {
      if (!(error instanceof DOMException && error.name === "AbortError")) {
        console.error("Erro ao buscar dados do dashboard:", error);
      }
      throw error;
    }
  },
};
