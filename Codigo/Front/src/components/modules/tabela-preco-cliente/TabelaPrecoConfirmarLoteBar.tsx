"use client";

import { useState } from "react";
import { showError, showSuccess } from "@/utils/toastUtils";

interface TabelaPrecoConfirmarLoteBarProps {
  pendentes: number;
  onConfirmarEmLote: () => Promise<number>;
  onConfirmarTabela: () => Promise<void>;
}

export default function TabelaPrecoConfirmarLoteBar({
  pendentes,
  onConfirmarEmLote,
  onConfirmarTabela,
}: TabelaPrecoConfirmarLoteBarProps) {
  const [processando, setProcessando] = useState(false);

  const confirmarLote = async () => {
    setProcessando(true);
    try {
      const confirmados = await onConfirmarEmLote();
      showSuccess(
        confirmados > 0
          ? `${confirmados} item(ns) de alta confiança confirmado(s) automaticamente.`
          : "Nenhum item elegível pra confirmação em lote — cada um exige reconfirmar que já reproduz um vínculo anterior.",
      );
    } catch (err) {
      showError(
        err instanceof Error ? err.message : "Falha na confirmação em lote.",
      );
    } finally {
      setProcessando(false);
    }
  };

  const confirmarTabela = async () => {
    setProcessando(true);
    try {
      await onConfirmarTabela();
      showSuccess("Tabela de preços confirmada.");
    } catch (err) {
      showError(
        err instanceof Error
          ? err.message
          : "Falha ao confirmar a tabela de preços.",
      );
    } finally {
      setProcessando(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 flex flex-wrap items-center justify-between gap-3">
      <p className="text-sm text-gray-600">
        {pendentes > 0
          ? `${pendentes} item(ns) ainda precisam de decisão manual.`
          : "Todos os itens já têm uma decisão — pronta pra confirmar."}
      </p>
      <div className="flex gap-2">
        <button
          type="button"
          onClick={confirmarLote}
          disabled={processando || pendentes === 0}
          className="py-2 px-3 text-sm border border-gray-300 text-gray-700 rounded disabled:opacity-50 cursor-pointer hover:bg-gray-50"
        >
          Confirmar em lote (alta confiança já conhecida)
        </button>
        <button
          type="button"
          onClick={confirmarTabela}
          disabled={processando || pendentes > 0}
          className="py-2 px-3 text-sm bg-primary text-white rounded disabled:opacity-50 cursor-pointer hover:bg-[var(--primary-dark)]"
          title={
            pendentes > 0
              ? "Ainda existem itens sugeridos sem confirmação humana"
              : undefined
          }
        >
          Confirmar tabela
        </button>
      </div>
    </div>
  );
}
