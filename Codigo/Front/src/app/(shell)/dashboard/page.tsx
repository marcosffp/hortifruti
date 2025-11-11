"use client";

import RoleGuard from "@/components/auth/RoleGuard";
import CashFlow from "@/components/modules/CashFlow";
import { Lock } from "lucide-react";
import Card from "@/components/ui/Card";
import Alerts from "@/components/ui/Alerts";
import { useState } from "react";
import { useReport } from "@/hooks/useReport";

export default function Dashboard() {
  const [showModalReport, setShowModalReport] = useState(false);
  const { isGenerating, error, generateReport } = useReport();

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

  const handleGenerateReport = (type: "MONTH" | "RANGE") => {
    setShowModalReport(false);

    if(type === "RANGE") {
      generateReport(startDate, endDate);
    } else if(type === "MONTH") {
      const now = new Date();
      const firstDay = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const lastDay = new Date(now.getFullYear(), now.getMonth(), 0);
      const startDate = firstDay.toISOString().split('T')[0];
      const endDate = lastDay.toISOString().split('T')[0];
      generateReport(startDate, endDate);
    }

    generateReport(startDate, endDate);

    console.log(`Gerando relatório do tipo: ${type}`);
  }

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

        <RoleGuard roles={["MANAGER"]} ignoreRedirect={true}>
          <Card title="Relatórios Financeiros">
            <p className="text-gray-600">
              Clique aqui para baixar seu relatório fiscal mensal em PDF.
            </p>
            <button 
              className="mt-4 px-4 py-2 bg-[var(--primary)] text-white rounded hover:bg-green-700"
              onClick={() => {
                setShowModalReport(true);
              }}
            >
              Baixar Relatório
            </button>
          </Card>
        </RoleGuard>
      </div>

      {/* Dashboard com Gráficos - Protegido para MANAGER */}
      <RoleGuard roles={["MANAGER"]} ignoreRedirect={true}>
        <CashFlow 
          startDate={startDate}
          endDate={endDate}
          setStartDate={setStartDate}
          setEndDate={setEndDate}
        />
      </RoleGuard>

      {/* Fallback para usuários sem permissão */}
      <RoleGuard
        roles={["MANAGER"]}
        ignoreRedirect={true}
        fallback={
          <>
            <div className="bg-white border border-gray-300 rounded-lg shadow-sm p-8 text-center mb-2">
              <Lock size={48} className="mx-auto text-gray-400 mb-4" />
              <h3 className="text-xl font-semibold text-gray-700 mb-2">
                Acesso Restrito
              </h3>
              <p className="text-gray-500 mb-4">
                Os relatórios financeiros são acessíveis apenas para usuários com perfil de Gerente.
              </p>
              <p className="text-sm text-gray-400">
                Entre em contato com um administrador para solicitar acesso.
              </p>
            </div>
            <Alerts></Alerts>
          </>
        }
      >
        {null}
      </RoleGuard>

      {showModalReport && (
        // Modal para seleção de relatório (MENSAL ou POR PERÍODO)
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-96">
            <h2 className="text-xl font-semibold mb-4">Selecionar Tipo de Relatório</h2>
            <div className="flex flex-col space-y-4">
              <div className="flex gap-4">
                <button
                className="px-4 py-2 bg-[var(--primary)] text-white rounded hover:bg-green-700"
                onClick={() => {
                  handleGenerateReport("MONTH");
                }}
              >
                Relatório Mensal
              </button>
              <button
                className="px-4 py-2 bg-[var(--primary)] text-white rounded hover:bg-green-700"
                onClick={() => {
                  handleGenerateReport("RANGE");
                }}
              >
                Relatório por Período
              </button>
              </div>
              <button
                className="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400"
                onClick={() => setShowModalReport(false)}
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
