"use client";

import CashFlow from "@/components/modules/CashFlow";
import Card from "@/components/ui/Card";

export default function Dashboard() {
  return (
    <main className="flex-1 p-6 bg-gray-50 overflow-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800">Dashboard</h1>
        <p className="text-gray-600">
          Visão geral dos dados financeiros e operacionais
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <Card title="Bem-vindo ao Hortifruti SL">
          <p className="text-gray-600">
            Sistema de gestão para hortifruti com módulos integrados para
            controle financeiro, gestão de estoque, vendas e muito mais.
          </p>
        </Card>
      </div>

      {/* Dashboard com Gráficos */}
      <CashFlow />
    </main>
  );
}
