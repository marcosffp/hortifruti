import type { TooltipItem } from "chart.js";

// Helper para truncar labels longas nos eixos (evita "estourar" a largura)
export const truncate = (text: string, max = 18) =>
  text.length > max ? `${text.slice(0, max)}…` : text;

// Compartilhado pelo gráfico de linha (Fluxo de Caixa Mensal) e pelo gráfico de
// barras (Receitas por Tipo de Venda) — ambos usam o mesmo layout vertical padrão.
export const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  layout: { padding: { left: 0, right: 0, top: 0, bottom: 0 } },
  plugins: {
    legend: {
      position: "bottom" as const,
      labels: {
        font: { size: 10 },
        boxWidth: 10,
        padding: 8,
      },
    },
    tooltip: {
      callbacks: {
        label: (context: TooltipItem<"bar"> | TooltipItem<"line">) => {
          const label = context.dataset.label || "";
          const value = context.parsed.y ?? 0;
          return `${label}: R$ ${value.toLocaleString("pt-BR", {
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
        callback: (value: number | string) =>
          `R$ ${Number(value).toLocaleString("pt-BR")}`,
      },
    },
  },
};
