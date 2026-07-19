"use client";

import { AlertCircle, Landmark, Loader2, RefreshCw } from "lucide-react";
import Card from "@/components/ui/Card";
import { useBankBalance } from "@/hooks/useBankBalance";

export default function BankBalanceCard() {
  const { balance, isLoading, error, refetch } = useBankBalance();

  const atualizadoAs = balance
    ? new Date(balance.consultadoEm.substring(0, 23)).toLocaleTimeString(
        "pt-BR",
        {
          hour: "2-digit",
          minute: "2-digit",
        },
      )
    : null;

  return (
    <Card>
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-2">
          <Landmark className="w-5 h-5 text-[var(--primary)]" />
          <h2 className="text-lg font-semibold">
            Saldo Disponível — Banco do Brasil
          </h2>
        </div>
        <button
          type="button"
          onClick={refetch}
          disabled={isLoading}
          className="p-1.5 rounded-md hover:bg-gray-100 text-gray-500 disabled:opacity-60"
          aria-label="Atualizar saldo"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin" : ""}`} />
        </button>
      </div>

      {isLoading && !balance && (
        <div className="flex items-center gap-2 text-gray-500 mt-2">
          <Loader2 className="w-4 h-4 animate-spin" />
          Consultando saldo...
        </div>
      )}

      {error && (
        <div className="flex items-start gap-2 rounded-md border border-red-200 bg-red-50 p-2 text-red-700 mt-2">
          <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {balance && (
        <div className="bg-green-50 p-4 rounded-lg mt-2">
          <p className="text-3xl font-bold text-green-600">
            R${" "}
            {balance.saldoDisponivel.toLocaleString("pt-BR", {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}
          </p>
          <p className="text-xs text-gray-500 mt-1">
            Atualizado às {atualizadoAs}
            {!balance.detalhado &&
              " · saldo de fechamento do dia (a API do BB não retornou o detalhamento oficial)"}
          </p>
        </div>
      )}
    </Card>
  );
}
