"use client";

import RoleGuard from "@/components/auth/RoleGuard";
import CashFlow from "@/components/modules/CashFlow";
import { Lock, Loader2, AlertCircle, CalendarDays, CalendarRange, X } from "lucide-react";
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

    if (type === "RANGE") {
      generateReport(startDate, endDate);
    } else if (type === "MONTH") {
      const now = new Date();
      const firstDay = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const lastDay = new Date(now.getFullYear(), now.getMonth(), 0);
      const start = firstDay.toISOString().split('T')[0];
      const end = lastDay.toISOString().split('T')[0];
      generateReport(start, end);
    }
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
            <div className="space-y-3">
              <p className="text-gray-600">
                Baixe seu relatório fiscal em PDF por mês anterior ou por um período específico.
              </p>

              <button
                className="mt-2 inline-flex items-center gap-2 px-4 py-2 bg-[var(--primary)] text-white rounded hover:bg-green-700 disabled:opacity-60 disabled:cursor-not-allowed"
                onClick={() => setShowModalReport(true)}
                disabled={isGenerating}
              >
                {isGenerating ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Gerando relatório...
                  </>
                ) : (
                  <>Baixar Relatório</>
                )}
              </button>

              {error && (
                <div className="flex items-start gap-2 rounded-md border border-red-200 bg-red-50 p-2 text-red-700">
                  <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
                  <span className="text-sm">{error}</span>
                </div>
              )}

              {isGenerating && (
                <p className="text-xs text-gray-500">
                  Seu relatório está sendo preparado. Isso pode levar alguns segundos.
                </p>
              )}
            </div>
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
        // Modal aprimorado para seleção de relatório (MENSAL ou POR PERÍODO)
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          {/* Backdrop */}
          <div
            className="absolute inset-0 bg-black/50 backdrop-blur-sm"
            onClick={() => !isGenerating && setShowModalReport(false)}
          />
          {/* Dialog */}
          <div className="relative z-10 w-full max-w-lg mx-4 bg-white rounded-2xl shadow-2xl border border-gray-100">
            {/* Header */}
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <div>
                <h2 className="text-lg sm:text-xl font-semibold text-gray-800">Selecionar Tipo de Relatório</h2>
                <p className="text-sm text-gray-500 mt-0.5">
                  Escolha entre o relatório do mês anterior ou um intervalo personalizado.
                </p>
              </div>
              <button
                className="p-2 rounded-md hover:bg-gray-100 text-gray-500"
                onClick={() => setShowModalReport(false)}
                disabled={isGenerating}
                aria-label="Fechar modal"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Content */}
            <div className="px-5 py-5 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  className="group rounded-xl border border-gray-200 hover:border-green-300 bg-white hover:bg-green-50 p-4 text-left transition-all disabled:opacity-60"
                  onClick={() => handleGenerateReport("MONTH")}
                  disabled={isGenerating}
                >
                  <div className="flex items-center gap-3">
                    <span className="inline-flex items-center justify-center w-10 h-10 rounded-lg bg-green-100 text-green-700">
                      <CalendarDays className="w-5 h-5" />
                    </span>
                    <div>
                      <p className="font-medium text-gray-800">Relatório Mensal</p>
                      <p className="text-xs text-gray-500">
                        Gera o .zip do mês anterior completo.
                      </p>
                    </div>
                  </div>
                </button>

                <button
                  className="group rounded-xl border border-gray-200 hover:border-green-300 bg-white hover:bg-green-50 p-4 text-left transition-all disabled:opacity-60"
                  onClick={() => handleGenerateReport("RANGE")}
                  disabled={isGenerating}
                >
                  <div className="flex items-center gap-3">
                    <span className="inline-flex items-center justify-center w-10 h-10 rounded-lg bg-green-100 text-green-700">
                      <CalendarRange className="w-5 h-5" />
                    </span>
                    <div>
                      <p className="font-medium text-gray-800">Relatório por Período</p>
                      <p className="text-xs text-gray-500">
                        Usa as datas selecionadas no filtro do dashboard.
                      </p>
                    </div>
                  </div>
                </button>
              </div>

              {isGenerating && (
                <div className="flex items-center gap-2 text-sm text-gray-600 bg-gray-50 border border-gray-200 rounded-md p-2">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Preparando seu relatório...
                </div>
              )}
            </div>

            {/* Footer */}
            <div className="px-5 py-4 border-t border-gray-100 flex justify-end">
              <button
                className="px-4 py-2 text-sm rounded-md border border-gray-300 hover:bg-gray-50 disabled:opacity-60"
                onClick={() => setShowModalReport(false)}
                disabled={isGenerating}
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
