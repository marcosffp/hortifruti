"use client";

import { useEffect, useState } from "react";
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

export default function CashFlow() {
  const { isLoading, error, getDashboardData } = useDashboard();
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);

  // Estados para filtros
  const [startDate, setStartDate] = useState(() => {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    return firstDay.toISOString().split('T')[0];
  });
  
  const [endDate, setEndDate] = useState(() => {
    const now = new Date();
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    return lastDay.toISOString().split('T')[0];
  });

  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());

  const fetchDashboardData = async () => {
    const data = await getDashboardData(startDate, endDate, selectedMonth, selectedYear);
    if (data) {
      setDashboardData(data);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, [startDate, endDate, selectedMonth, selectedYear]);

  // Dados para gráfico de linha (Fluxo de Caixa)
  const lineChartData = {
    labels: dashboardData ? Object.keys(dashboardData.FluxoDeCaixa) : [],
    datasets: [
      {
        label: 'Receitas',
        data: dashboardData ? Object.values(dashboardData.FluxoDeCaixa).map(item => item.Receitas || 0) : [],
        borderColor: 'rgb(34, 197, 94)',
        backgroundColor: 'rgba(34, 197, 94, 0.2)',
        tension: 0.1,
      },
      {
        label: 'Despesas',
        data: dashboardData ? Object.values(dashboardData.FluxoDeCaixa).map(item => Math.abs(item.Despesas || 0)) : [],
        borderColor: 'rgb(239, 68, 68)',
        backgroundColor: 'rgba(239, 68, 68, 0.2)',
        tension: 0.1,
      },
    ],
  };

  // Dados para gráfico de barras (Receitas por Tipo)
  const barChartData = {
    labels: ['Vendas Cartão', 'Vendas PIX'],
    datasets: [
      {
        label: 'Receitas por Tipo',
        data: dashboardData ? [
          dashboardData.ReceitasPorTipo.VendasCartao,
          dashboardData.ReceitasPorTipo.VendasPix
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
    labels: dashboardData ? Object.keys(dashboardData.PorcentagemPorCategoria) : [],
    datasets: [
      {
        data: dashboardData ? Object.values(dashboardData.PorcentagemPorCategoria).map(item => item.Porcentagem) : [],
        backgroundColor: [
          '#FF6384',
          '#36A2EB',
          '#FFCE56',
          '#4BC0C0',
          '#9966FF',
          '#FF9F40',
          '#FF6384',
          '#C9CBCF',
        ],
        hoverBackgroundColor: [
          '#FF6384',
          '#36A2EB',
          '#FFCE56',
          '#4BC0C0',
          '#9966FF',
          '#FF9F40',
          '#FF6384',
          '#C9CBCF',
        ],
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top' as const,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
      },
    },
  };

  const pieOptions = {
    responsive: true,
    maintainAspectRatio: false, // Adicione esta linha
    plugins: {
      legend: {
        position: 'bottom' as const,
        labels: {
          boxWidth: 12,
          padding: 10,
          font: {
            size: 12,
          },
        },
      },
      tooltip: {
        callbacks: {
          label: function(context: any) {
            const label = context.label || '';
            const value = context.parsed;
            return `${label}: ${value.toFixed(1)}%`;
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

  return (
    <div className="grid gap-6">
      {/* Filtros */}
      <Card title="Filtros">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
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
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-green-50 p-4 rounded-lg">
            <p className="text-sm text-gray-600">Total Receita</p>
            <p className="text-2xl font-bold text-green-600">
              R$ {dashboardData?.Totais.TotalReceita.toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              }) || '0,00'}
            </p>
          </div>
          <div className="bg-red-50 p-4 rounded-lg">
            <p className="text-sm text-gray-600">Total Custos</p>
            <p className="text-2xl font-bold text-red-600">
              R$ {Math.abs(dashboardData?.Totais.TotalCusto || 0).toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </p>
          </div>
          <div className="bg-blue-50 p-4 rounded-lg">
            <p className="text-sm text-gray-600">Margem de Lucro</p>
            <p className="text-2xl font-bold text-blue-600">
              {dashboardData?.Totais.MargemLucro.toFixed(2) || '0,00'}%
            </p>
          </div>
          <div className="bg-purple-50 p-4 rounded-lg">
            <p className="text-sm text-gray-600">Saldo</p>
            <p className="text-2xl font-bold text-purple-600">
              R$ {((dashboardData?.Totais.TotalReceita || 0) - Math.abs(dashboardData?.Totais.TotalCusto || 0)).toLocaleString('pt-BR', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </p>
          </div>
        </div>
      </Card>

      {/* Gráficos */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Gráfico de Linha - Fluxo de Caixa */}
        <Card title="Fluxo de Caixa Mensal">
          <div className="h-64">
            <Line data={lineChartData} options={chartOptions} />
          </div>
        </Card>

        {/* Gráfico de Barras - Receitas por Tipo */}
        <Card title="Receitas por Tipo de Venda">
          <div className="h-64">
            <Bar data={barChartData} options={chartOptions} />
          </div>
        </Card>
      </div>

      {/* Gráfico de Pizza e Ranking */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Gráfico de Pizza - Categorias */}
        <Card title="Distribuição por Categoria">
          <div className="h-80 w-full flex items-center justify-center"> {/* Aumentei a altura e centralizei */}
            <div className="w-full h-full max-w-sm"> {/* Container com largura máxima */}
              <Pie data={pieChartData} options={pieOptions} />
            </div>
          </div>
        </Card>

        {/* Ranking de Categorias */}
        <Card title="Ranking de Gastos por Categoria">
          <div className="space-y-2 max-h-80 overflow-y-auto"> {/* Adicionei scroll caso tenha muitas categorias */}
            {dashboardData?.RankingCategoriasGastos.map((item, index) => (
              <div
                key={item.Categoria}
                className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
              >
                <div className="flex items-center space-x-3">
                  <span className="flex items-center justify-center w-6 h-6 bg-green-500 text-white rounded-full text-sm font-bold">
                    {item.Rank}
                  </span>
                  <span className="font-medium text-sm">{item.Categoria}</span> {/* Reduzi o tamanho da fonte */}
                </div>
                <span className="text-sm font-bold text-red-600"> {/* Reduzi o tamanho da fonte */}
                  R$ {item.Valor.toLocaleString('pt-BR', {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}
                </span>
              </div>
            ))}
            {(!dashboardData?.RankingCategoriasGastos || dashboardData.RankingCategoriasGastos.length === 0) && (
              <div className="text-center text-gray-500 py-8">
                Nenhum dado disponível
              </div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}
