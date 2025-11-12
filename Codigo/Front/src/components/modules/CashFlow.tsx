"use client";

import { useEffect, useRef, useState } from "react";
import { useDashboard } from "@/hooks/useDashboard";
import { DashboardData } from "@/services/dashboardService";
import Card from "@/components/ui/Card";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
} from 'chart.js';
import { Line, Bar, Pie } from 'react-chartjs-2';
import Loading from "@/components/ui/Loading";
import { TrendingUp, Package, BarChart3 } from "lucide-react";

// Registrar componentes do Chart.js
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement
);

// Mapeamento de meses em inglês para português
const monthNames: { [key: string]: string } = {
  'JANUARY': 'Janeiro',
  'FEBRUARY': 'Fevereiro',
  'MARCH': 'Março',
  'APRIL': 'Abril',
  'MAY': 'Maio',
  'JUNE': 'Junho',
  'JULY': 'Julho',
  'AUGUST': 'Agosto',
  'SEPTEMBER': 'Setembro',
  'OCTOBER': 'Outubro',
  'NOVEMBER': 'Novembro',
  'DECEMBER': 'Dezembro',
};

// Ordem dos meses para ordenação
const monthOrder: { [key: string]: number } = {
  'JANUARY': 1,
  'FEBRUARY': 2,
  'MARCH': 3,
  'APRIL': 4,
  'MAY': 5,
  'JUNE': 6,
  'JULY': 7,
  'AUGUST': 8,
  'SEPTEMBER': 9,
  'OCTOBER': 10,
  'NOVEMBER': 11,
  'DECEMBER': 12,
};

// Mapeamento de categorias para nomes mais amigáveis
const categoryNames: { [key: string]: string } = {
  'VENDAS_CARTAO': 'Vendas Cartão',
  'VENDAS_PIX': 'Vendas PIX',
  'SERVICOS_BANCARIOS': 'Serviços Bancários',
  'FORNECEDOR': 'Fornecedor',
  'FAMÍLIA': 'Família',
  'FUNCIONARIO': 'Funcionário',
  'FISCAL': 'Fiscal',
};

type CashFlowProps = {
  startDate: string;
  endDate: string;
  setStartDate: React.Dispatch<React.SetStateAction<string>>;
  setEndDate: React.Dispatch<React.SetStateAction<string>>;
}

export default function CashFlow({ startDate, endDate, setStartDate, setEndDate }: CashFlowProps) {
  const { isLoading, error, getDashboardData } = useDashboard();
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const debounceTimer = useRef<NodeJS.Timeout | null>(null);

  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());

  const fetchDashboardData = async () => {
    const data = await getDashboardData(startDate, endDate, selectedMonth, selectedYear);
    console.log("Dashboard Data:", data);
    if (data) {
      setDashboardData(data);
    }
  };

  useEffect(() => {
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
    }

    debounceTimer.current = setTimeout(() => {
      fetchDashboardData();
    }, 500);

    return () => {
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
    }
  }, [startDate, endDate, selectedMonth, selectedYear]);

  // Dados para gráfico de linha (Fluxo de Caixa) - ORDENADO e com valores NEGATIVOS
  const lineChartData = (() => {
    if (!dashboardData?.FluxoDeCaixa) {
      return {
        labels: [],
        datasets: [
          {
            label: 'Receitas',
            data: [],
            borderColor: 'rgb(34, 197, 94)',
            backgroundColor: 'rgba(34, 197, 94, 0.2)',
            tension: 0.1,
          },
          {
            label: 'Despesas',
            data: [],
            borderColor: 'rgb(239, 68, 68)',
            backgroundColor: 'rgba(239, 68, 68, 0.2)',
            tension: 0.1,
          },
        ],
      };
    }

    // Ordenar os meses em ordem crescente (Janeiro -> Dezembro)
    const sortedMonths = Object.keys(dashboardData.FluxoDeCaixa).sort(
      (a, b) => (monthOrder[a] || 0) - (monthOrder[b] || 0)
    );

    const labels = sortedMonths.map(month => monthNames[month] || month);
    const receitas = sortedMonths.map(month => dashboardData.FluxoDeCaixa[month]?.Receitas || 0);
    // Manter valores negativos (não usar Math.abs)
    const despesas = sortedMonths.map(month => dashboardData.FluxoDeCaixa[month]?.Despesas || 0);

    return {
      labels,
      datasets: [
        {
          label: 'Receitas',
          data: receitas,
          borderColor: 'rgb(34, 197, 94)',
          backgroundColor: 'rgba(34, 197, 94, 0.2)',
          tension: 0.1,
        },
        {
          label: 'Despesas',
          data: despesas,
          borderColor: 'rgb(239, 68, 68)',
          backgroundColor: 'rgba(239, 68, 68, 0.2)',
          tension: 0.1,
        },
      ],
    };
  })();

  // Dados para gráfico de barras (Receitas por Tipo)
  const barChartData = {
    labels: ['Vendas Cartão', 'Vendas PIX'],
    datasets: [
      {
        label: 'Receitas (R$)',
        data: dashboardData ? [
          dashboardData.ReceitasPorTipo?.VendasCartao || 0,
          dashboardData.ReceitasPorTipo?.VendasPix || 0
        ] : [0, 0],
        backgroundColor: [
          'rgba(34, 197, 94, 0.8)',
          'rgba(59, 130, 246, 0.8)',
        ],
        borderColor: [
          'rgb(34, 197, 94)',
          'rgb(59, 130, 246)',
        ],
        borderWidth: 1,
      },
    ],
  };

  // Dados para gráfico de pizza (Porcentagem por Categoria)
  const pieChartData = {
    labels: dashboardData 
      ? Object.keys(dashboardData.PorcentagemPorCategoria || {}).map(
          key => categoryNames[key] || key
        )
      : [],
    datasets: [
      {
        data: dashboardData 
          ? Object.values(dashboardData.PorcentagemPorCategoria || {}).map(item => item.Porcentagem) 
          : [],
        backgroundColor: [
          '#FF6384', // VENDAS_CARTAO
          '#36A2EB', // SERVIÇOS_BANCARIOS
          '#FFCE56', // FORNECEDOR
          '#4BC0C0', // FAMÍLIA
          '#9966FF', // VENDAS_PIX
          '#FF9F40', // FUNCIONARIO
          '#C9CBCF', // FISCAL
        ],
        hoverBackgroundColor: [
          '#FF6384',
          '#36A2EB',
          '#FFCE56',
          '#4BC0C0',
          '#9966FF',
          '#FF9F40',
          '#C9CBCF',
        ],
      },
    ],
  };

  // Dados para gráfico de barras horizontais (Fluxo de Vendas)
  const salesFlowChartData = {
    labels: dashboardData 
      ? Object.keys(dashboardData["Fluxo de Vendas"] || {})
      : [],
    datasets: [
      {
        label: 'Vendas (R$)',
        data: dashboardData 
          ? Object.values(dashboardData["Fluxo de Vendas"] || {})
          : [],
        backgroundColor: 'rgba(59, 130, 246, 0.8)',
        borderColor: 'rgb(59, 130, 246)',
        borderWidth: 1,
      },
    ],
  };

  // Dados para gráfico de barras horizontais (Top 10 Produtos por Quantidade)
  const topProductsByQuantityChartData = {
    labels: dashboardData 
      ? dashboardData["Top10ProdutosPorQuantidade"]?.map(p => p.Nome) || []
      : [],
    datasets: [
      {
        label: 'Quantidade',
        data: dashboardData 
          ? dashboardData["Top10ProdutosPorQuantidade"]?.map(p => p.QuantidadeTotal) || []
          : [],
        backgroundColor: 'rgba(74, 222, 128, 0.8)',
        borderColor: 'rgb(34, 197, 94)',
        borderWidth: 1,
      },
    ],
  };

  // Helper para truncar labels longas nos eixos (evita "estourar" a largura)
  const truncate = (text: any, max = 18) =>
    typeof text === 'string' && text.length > max ? `${text.slice(0, max)}…` : text;

  // Opções padrão de gráficos (responsivo e sem manter proporção fixa)
  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    layout: { padding: { left: 0, right: 0, top: 0, bottom: 0 } }, // reduz espaços
    plugins: {
      legend: {
        position: 'bottom' as const,
        labels: {
          font: { size: 10 },
          boxWidth: 10,
          padding: 8,
        },
      },
      tooltip: {
        callbacks: {
          label: function (context: any) {
            const label = context.dataset.label || '';
            const value = context.parsed.y;
            return `${label}: R$ ${value.toLocaleString('pt-BR', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}`;
          },
        },
      },
    },
    scales: {
      x: {
        ticks: {
          autoSkip: true,
          maxTicksLimit: 6,
          font: { size: 10 },
        },
      },
      y: {
        beginAtZero: true,
        ticks: {
          autoSkip: true,
          maxTicksLimit: 6,
          font: { size: 10 },
          callback: function (value: any) {
            return 'R$ ' + Number(value).toLocaleString('pt-BR');
          },
        },
      },
    },
  };

  const horizontalBarOptions = {
    indexAxis: 'y' as const,
    responsive: true,
    maintainAspectRatio: false,
    layout: { padding: { left: 0, right: 0, top: 0, bottom: 0 } },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: function (context: any) {
            const value = context.parsed.x;
            return `R$ ${value.toLocaleString('pt-BR', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}`;
          },
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        ticks: {
          autoSkip: true,
          maxTicksLimit: 6,
          font: { size: 10 },
          callback: function (value: any) {
            return 'R$ ' + Number(value).toLocaleString('pt-BR');
          },
        },
      },
      y: {
        ticks: {
          autoSkip: true,
          maxTicksLimit: 10,
          font: { size: 10 },
          // Trunca labels de semanas se necessário
          callback: function (_val: any, index: number) {
            return truncate((salesFlowChartData.labels as any[])[index]);
          },
        },
      },
    },
  };

  const quantityBarOptions = {
    indexAxis: 'y' as const,
    responsive: true,
    maintainAspectRatio: false,
    layout: { padding: { left: 0, right: 0, top: 0, bottom: 0 } },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: function (context: any) {
            const value = context.parsed.x;
            return `${value} unidades`;
          },
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        ticks: {
          autoSkip: true,
          maxTicksLimit: 6,
          font: { size: 10 },
          callback: function (value: any) {
            return value + ' un.';
          },
        },
      },
      y: {
        ticks: {
          autoSkip: true,
          maxTicksLimit: 10,
          font: { size: 10 },
          // Trunca nomes de produtos longos (evita "passar" da tela)
          callback: function (_val: any, index: number) {
            return truncate((topProductsByQuantityChartData.labels as any[])[index]);
          },
        },
      },
    },
  };

  const pieOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom' as const,
        labels: {
          boxWidth: 12,
          padding: 10,
          font: {
            size: 11,
          },
        },
      },
      tooltip: {
        callbacks: {
          label: function(context: any) {
            const label = context.label || '';
            const percentage = context.parsed;
            const datasetIndex = context.datasetIndex;
            const dataIndex = context.dataIndex;
            
            // Pega o valor em reais do objeto original
            const categoryKey = Object.keys(dashboardData?.PorcentagemPorCategoria || {})[dataIndex];
            const valor = dashboardData?.PorcentagemPorCategoria?.[categoryKey]?.Valor || 0;
            
            return [
              `${label}: ${percentage.toFixed(2)}%`,
              `R$ ${valor.toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}`
            ];
          },
        },
      },
    },
    layout: {
      padding: {
        top: 10,
        bottom: 10,
        left: 10,
        right: 10,
      },
    },
  };

  if (isLoading) {
    return (
      <Loading />
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <p className="text-red-700">Erro ao carregar dados: {error}</p>
      </div>
    );
  }

  const fluxoVendasData = dashboardData?.["Fluxo de Vendas"] || {};
  const hasFluxoVendas = Object.keys(fluxoVendasData).length > 0;

  const produtosEmAlta = dashboardData?.["Produtos em Alta"] || [];
  const hasProdutosEmAlta = produtosEmAlta.length > 0;

  const top10ProdutosPorQuantidade = dashboardData?.["Top10ProdutosPorQuantidade"] || [];
  const hasTop10ProdutosPorQuantidade = top10ProdutosPorQuantidade.length > 0;

  return (
    <div className="grid gap-4 sm:gap-6 w-full max-w-full overflow-x-hidden">
      {/* Filtros */}
      <Card title="Filtros">
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 sm:gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Data Inicial
            </label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Data Final
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Mês
            </label>
            <select
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(Number(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            >
              {Array.from({ length: 12 }, (_, i) => (
                <option key={i + 1} value={i + 1}>
                  {new Date(0, i).toLocaleString('pt-BR', { month: 'long' }).replace(/^\w/, c => c.toUpperCase())}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Ano
            </label>
            <input
              type="number"
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
        </div>
      </Card>

      {/* Cards de Resumo */}
      <Card title="Resumo Financeiro">
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 sm:gap-4">
          <div className="bg-green-50 p-4 rounded-lg">
            <p className="text-sm text-gray-600">Total Receita</p>
            <p className="text-2xl font-bold text-green-600">
              R$ {(dashboardData?.Totais?.TotalReceita || 0).toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </p>
          </div>
          <div className="bg-red-50 p-4 rounded-lg">
            <p className="text-sm text-gray-600">Total Custos</p>
            <p className="text-2xl font-bold text-red-600">
              R$ {Math.abs(dashboardData?.Totais?.TotalCusto || 0).toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </p>
          </div>
          <div className="bg-blue-50 p-4 rounded-lg">
            <p className="text-sm text-gray-600">Margem de Lucro</p>
            <p className="text-2xl font-bold text-blue-600">
              {(dashboardData?.Totais?.MargemLucro || 0).toFixed(2)}%
            </p>
          </div>
          <div className="bg-purple-50 p-4 rounded-lg">
            <p className="text-sm text-gray-600">Saldo</p>
            <p className={`text-2xl font-bold ${
              ((dashboardData?.Totais?.TotalReceita || 0) - Math.abs(dashboardData?.Totais?.TotalCusto || 0)) >= 0 
                ? 'text-purple-600' 
                : 'text-red-600'
            }`}>
              R$ {((dashboardData?.Totais?.TotalReceita || 0) - Math.abs(dashboardData?.Totais?.TotalCusto || 0)).toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </p>
          </div>
        </div>
      </Card>

      {/* Gráficos Principais */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        {/* Gráfico de Linha - Fluxo de Caixa */}
        <Card title="Fluxo de Caixa Mensal">
          <div className="h-56 sm:h-64 md:h-72 w-full overflow-hidden">
            {dashboardData?.FluxoDeCaixa && Object.keys(dashboardData.FluxoDeCaixa).length > 0 ? (
              <Line data={lineChartData} options={chartOptions} style={{ width: '100%', height: '100%' }} />
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-gray-500">
                <p>Nenhum dado de fluxo de caixa disponível.</p>
                <p className="text-sm">Tente alterar o intervalo de datas no filtro.</p>
              </div>
            )}
          </div>
        </Card>

        {/* Gráfico de Barras - Receitas por Tipo */}
        <Card title="Receitas por Tipo de Venda">
          <div className="h-56 sm:h-64 md:h-72 w-full overflow-hidden">
            {dashboardData?.ReceitasPorTipo ? (
              <Bar data={barChartData} options={chartOptions} style={{ width: '100%', height: '100%' }} />
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-gray-500">
                <p>Nenhum dado de receitas disponível.</p>
                <p className="text-sm">Tente alterar o intervalo de datas no filtro.</p>
              </div>
            )}
          </div>
        </Card>
      </div>

      {/* Gráfico de Pizza e Ranking */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        {/* Gráfico de Pizza - Categorias */}
        <Card title="Distribuição por Categoria">
          <div className="h-72 sm:h-80 w-full flex items-center justify-center overflow-hidden">
            <div className="w-full h-full max-w-full">
              {dashboardData?.PorcentagemPorCategoria && Object.keys(dashboardData.PorcentagemPorCategoria).length > 0 ? (
                <Pie data={pieChartData} options={pieOptions} style={{ width: '100%', height: '100%' }} />
              ) : (
                <div className="flex flex-col items-center justify-center h-full text-gray-500">
                  <p>Nenhum dado de categorias disponível.</p>
                  <p className="text-sm">Tente alterar o intervalo de datas no filtro.</p>
                </div>
              )}
            </div>
          </div>
        </Card>

        {/* Ranking de Categorias */}
        <Card title="Ranking de Gastos por Categoria">
          <div className="space-y-2 max-h-60 sm:max-h-80 overflow-y-auto">
            {dashboardData?.RankingCategoriasGastos && dashboardData.RankingCategoriasGastos.length > 0 ? (
              dashboardData.RankingCategoriasGastos.map((item) => (
                <div
                  key={item.Categoria}
                  className="flex items-center justify-between p-3 bg-gradient-to-r from-lime-50 to-green-50 rounded-lg hover:from-orange-100 hover:to-yellow-100 transition-colors border border-orange-200"
                >
                  <div className="flex items-center space-x-3">
                    <span className="flex items-center justify-center w-8 h-8 bg-gradient-to-br from-orange-500 to-yellow-500 text-white rounded-full text-sm font-bold shadow-sm">
                      {item.Rank}
                    </span>
                    <span className="font-medium text-sm">
                      {categoryNames[item.Categoria] || item.Categoria}
                    </span>
                  </div>
                  <span className="text-sm font-bold text-red-600">
                    R$ {item.Valor.toLocaleString('pt-BR', {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </span>
                </div>
              ))
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-gray-500">
                <p>Nenhum dado de ranking disponível.</p>
                <p className="text-sm">Tente alterar o campo <strong>Mês</strong> no filtro de datas.</p>
              </div>
            )}
          </div>
        </Card>
      </div>

      {/* Fluxo de Vendas e Produtos em Alta */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        {/* Fluxo de Vendas Semanal */}
        <Card title="Fluxo de Vendas (Semanal)">
          <div className="h-72 sm:h-80 w-full overflow-hidden">
            {hasFluxoVendas ? (
              <Bar data={salesFlowChartData} options={horizontalBarOptions} style={{ width: '100%', height: '100%' }} />
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-gray-500">
                <TrendingUp className="w-12 h-12 mb-3 text-gray-400" />
                <p className="text-center">Nenhum dado de vendas disponível</p>
                <p className="text-xs text-center mt-1">Crie agrupamentos de compras para visualizar o fluxo de vendas</p>
              </div>
            )}
          </div>
        </Card>

        {/* Produtos em Alta */}
        <Card title="Produtos em Alta (Top 10)">
          <div className="space-y-2 max-h-60 sm:max-h-80 overflow-y-auto">
            {hasProdutosEmAlta ? (
              produtosEmAlta.map((produto, index) => (
                <div
                  key={`${produto.Nome}-${index}`}
                  className="flex items-center justify-between p-3 bg-gradient-to-r from-lime-50 to-green-50 rounded-lg hover:from-orange-100 hover:to-yellow-100 transition-colors border border-orange-200"
                >
                  <div className="flex items-center space-x-3 flex-1 min-w-0">
                    <span className="flex items-center justify-center w-8 h-8 bg-gradient-to-br from-orange-500 to-yellow-500 text-white rounded-full text-sm font-bold shadow-sm">
                      {index + 1}
                    </span>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-sm text-gray-800 truncate">
                        {produto.Nome}
                      </p>
                      <p className="text-xs text-gray-600">
                        Quantidade: {produto.QuantidadeTotal} un.
                      </p>
                    </div>
                  </div>
                  <div className="text-right ml-3">
                    <p className="text-sm font-bold text-orange-600">
                      R$ {produto.ValorTotal.toLocaleString('pt-BR', {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}
                    </p>
                  </div>
                </div>
              ))
            ) : (
              <div className="flex flex-col items-center justify-center h-64 text-gray-500">
                <Package className="w-12 h-12 mb-3 text-gray-400" />
                <p className="text-center">Nenhum produto em destaque</p>
                <p className="text-xs text-center mt-1">Aguarde o processamento dos dados de vendas</p>
              </div>
            )}
          </div>
        </Card>
      </div>

      {/* Top 10 Produtos por Quantidade */}
      <Card title="Top 10 Produtos Mais Vendidos (por Quantidade)">
        <div className="h-[360px] sm:h-96 w-full overflow-hidden">
          {hasTop10ProdutosPorQuantidade ? (
            <Bar data={topProductsByQuantityChartData} options={quantityBarOptions} style={{ width: '100%', height: '100%' }} />
          ) : (
            <div className="flex flex-col items-center justify-center h-full text-gray-500">
              <BarChart3 className="w-12 h-12 mb-3 text-gray-400" />
              <p className="text-center">Nenhum dado de produtos disponível</p>
              <p className="text-xs text-center mt-1">Registre vendas para visualizar os produtos mais vendidos por quantidade</p>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}