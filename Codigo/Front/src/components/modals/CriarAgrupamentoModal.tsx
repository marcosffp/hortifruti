"use client";

import { X } from "lucide-react";
import { useEffect, useState } from "react";
import { useCombinedScore } from "@/hooks/useCombinedScore";
import {
  getLastMonthInterval,
  getWeekInterval,
  todaySaoPaulo,
} from "@/utils/dateUtils";
import { showError, showSuccess } from "@/utils/toastUtils";

interface CriarAgrupamentoModalProps {
  clientId: number;
  onClose: () => void;
  onCreated?: () => void;
}

export default function CriarAgrupamentoModal({
  clientId,
  onClose,
  onCreated,
}: CriarAgrupamentoModalProps) {
  const [groupBy, setGroupBy] = useState<"week" | "month" | "custom">("custom");
  const [creatingGrouping, setCreatingGrouping] = useState(false);
  const [startDate, setStartDate] = useState(() => todaySaoPaulo());
  const [endDate, setEndDate] = useState(() => {
    const now = new Date();
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    return lastDay.toISOString().split("T")[0];
  });
  const [confirmedAt, setConfirmedAt] = useState(() => todaySaoPaulo());

  const { createCombinedScore } = useCombinedScore();

  useEffect(() => {
    if (groupBy === "week") {
      const { start, end } = getWeekInterval();
      setStartDate(start);
      setEndDate(end);
    } else if (groupBy === "month") {
      const { start, end } = getLastMonthInterval();
      setStartDate(start);
      setEndDate(end);
    }
  }, [groupBy]);

  const handleConfirmGrouping = async () => {
    if (!clientId) {
      showError("Selecione um cliente primeiro");
      return;
    }

    if (!startDate || !endDate) {
      showError("Selecione o período (data início e fim)");
      return;
    }

    if (new Date(startDate) > new Date(endDate)) {
      showError("A data inicial não pode ser posterior à data final");
      return;
    }

    setCreatingGrouping(true);
    try {
      await createCombinedScore({
        clientId,
        startDate,
        endDate,
        confirmedAt,
      });
      showSuccess(
        "Agrupamento criado com sucesso! Veja na aba 'Produtos Agrupados'",
      );
      onCreated?.();
      onClose();
    } catch (error) {
      showError(
        error instanceof Error ? error.message : "Erro ao criar agrupamento",
      );
      console.error(error);
    } finally {
      setCreatingGrouping(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl">
        <div className="flex justify-between items-center p-6 border-b border-gray-300">
          <h3 className="text-xl font-semibold">
            Criar Agrupamento por Período
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label
              htmlFor="purchase-files-groupby"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              Tipo de Agrupamento
            </label>
            <select
              id="purchase-files-groupby"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              value={groupBy}
              onChange={(e) =>
                setGroupBy(e.target.value as "week" | "month" | "custom")
              }
            >
              <option value="custom">Intervalo Personalizado</option>
              <option value="week">Semanal (Última Semana)</option>
              <option value="month">Mensal (Mês Passado)</option>
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label
                htmlFor="purchase-files-data-inicial"
                className="block text-sm font-medium text-gray-700 mb-2"
              >
                Data Inicial
              </label>
              <input
                id="purchase-files-data-inicial"
                type="date"
                value={startDate}
                disabled={groupBy !== "custom"}
                onChange={(e) => setStartDate(e.target.value)}
                className={`w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 ${
                  groupBy !== "custom" ? "bg-gray-100 cursor-not-allowed" : ""
                }`}
              />
            </div>
            <div>
              <label
                htmlFor="purchase-files-data-final"
                className="block text-sm font-medium text-gray-700 mb-2"
              >
                Data Final
              </label>
              <input
                id="purchase-files-data-final"
                type="date"
                value={endDate}
                disabled={groupBy !== "custom"}
                onChange={(e) => setEndDate(e.target.value)}
                className={`w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 ${
                  groupBy !== "custom" ? "bg-gray-100 cursor-not-allowed" : ""
                }`}
              />
            </div>
          </div>

          <div>
            <label
              htmlFor="purchase-files-data-confirmacao"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              Data de Confirmação
            </label>
            <input
              id="purchase-files-data-confirmacao"
              type="date"
              value={confirmedAt}
              onChange={(e) => setConfirmedAt(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            />
            <p className="text-xs text-gray-500 mt-1">
              Data que será registrada como confirmação do agrupamento. Deixe
              como hoje se não tiver preferência.
            </p>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-800">
              <strong>Período selecionado:</strong>{" "}
              {new Date(startDate.split("-").join("/")).toLocaleDateString(
                "pt-BR",
              )}{" "}
              até{" "}
              {new Date(endDate.split("-").join("/")).toLocaleDateString(
                "pt-BR",
              )}
            </p>
            <p className="text-xs text-blue-600 mt-1">
              Todos os arquivos de compra dentro deste período serão agrupados.
            </p>
          </div>
        </div>

        <div className="flex justify-end gap-3 p-6 border-t border-gray-300">
          <button
            type="button"
            onClick={onClose}
            disabled={creatingGrouping}
            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={handleConfirmGrouping}
            disabled={creatingGrouping || !clientId}
            className={`px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors ${
              creatingGrouping || !clientId
                ? "opacity-50 cursor-not-allowed"
                : ""
            }`}
          >
            {creatingGrouping ? "Criando..." : "Confirmar Agrupamento"}
          </button>
        </div>
      </div>
    </div>
  );
}
