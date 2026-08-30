"use client";

import { useState } from "react";
import { showError } from "@/utils/toastUtils";

interface TabelaPrecoExportButtonsProps {
  onExportarCsv: () => Promise<void>;
  onExportarPdf: () => Promise<void>;
}

export default function TabelaPrecoExportButtons({
  onExportarCsv,
  onExportarPdf,
}: TabelaPrecoExportButtonsProps) {
  const [exportando, setExportando] = useState(false);

  const exportar = async (fn: () => Promise<void>) => {
    setExportando(true);
    try {
      await fn();
    } catch (err) {
      showError(err instanceof Error ? err.message : "Falha ao exportar.");
    } finally {
      setExportando(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 flex items-center justify-between gap-3">
      <p className="text-sm text-gray-600">
        Tabela confirmada — exporte pro formato interno da loja.
      </p>
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => exportar(onExportarCsv)}
          disabled={exportando}
          className="py-2 px-3 text-sm border border-gray-300 text-gray-700 rounded disabled:opacity-50 cursor-pointer hover:bg-gray-50"
        >
          Exportar CSV
        </button>
        <button
          type="button"
          onClick={() => exportar(onExportarPdf)}
          disabled={exportando}
          className="py-2 px-3 text-sm border border-gray-300 text-gray-700 rounded disabled:opacity-50 cursor-pointer hover:bg-gray-50"
        >
          Exportar PDF
        </button>
      </div>
    </div>
  );
}
