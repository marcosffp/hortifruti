"use client";

import RoleGuard from "@/components/auth/RoleGuard";
import CashFlow from "@/components/modules/CashFlow";
import { Lock } from "lucide-react";
import Card from "@/components/ui/Card";
import Alerts from "@/components/ui/Alerts";

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

      {/* Dashboard com Gráficos - Protegido para MANAGER */}
      <RoleGuard roles={["MANAGER"]} ignoreRedirect={true}>
        <CashFlow />
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
    </main>
  );
}
