"use client";

import { useEffect, useState } from "react";
import { useFiscalProduct } from "@/hooks/useFiscalProduct";
import type { FiscalProductType } from "@/types/purchaseType";
import type { TabelaPrecoClienteResponse } from "@/types/tabelaPrecoClienteType";
import { showError } from "@/utils/toastUtils";
import TabelaPrecoConfirmarLoteBar from "./TabelaPrecoConfirmarLoteBar";
import TabelaPrecoExportButtons from "./TabelaPrecoExportButtons";
import TabelaPrecoItemRow from "./TabelaPrecoItemRow";

const STATUS_LABEL: Record<TabelaPrecoClienteResponse["status"], string> = {
  RASCUNHO: "Rascunho — revisão ainda não iniciada",
  EM_REVISAO: "Em revisão",
  CONFIRMADA: "Confirmada",
};

interface TabelaPrecoItensReviewProps {
  tabela: TabelaPrecoClienteResponse;
  onConfirmarItem: (itemId: number, code: string) => Promise<void>;
  onSemCorrespondencia: (itemId: number) => Promise<void>;
  onConfirmarEmLote: () => Promise<number>;
  onConfirmarTabela: () => Promise<void>;
  onExportarCsv: () => Promise<void>;
  onExportarPdf: () => Promise<void>;
}

export default function TabelaPrecoItensReview({
  tabela,
  onConfirmarItem,
  onSemCorrespondencia,
  onConfirmarEmLote,
  onConfirmarTabela,
  onExportarCsv,
  onExportarPdf,
}: TabelaPrecoItensReviewProps) {
  const [products, setProducts] = useState<FiscalProductType[]>([]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const { getFiscalProducts } = useFiscalProduct();

  useEffect(() => {
    getFiscalProducts()
      .then(setProducts)
      .catch((err) =>
        showError(
          err instanceof Error ? err.message : "Falha ao carregar o catálogo.",
        ),
      )
      .finally(() => setLoadingProducts(false));
  }, [getFiscalProducts]);

  const tabelaConfirmada = tabela.status === "CONFIRMADA";
  const pendentes = tabela.itens.filter(
    (item) => item.statusMatch === "SUGERIDO",
  ).length;

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 flex items-center justify-between">
        <div>
          <p className="text-sm font-semibold text-gray-800">
            Competência {String(tabela.competenciaMes).padStart(2, "0")}/
            {tabela.competenciaAno} — versão {tabela.versao}
          </p>
          <p className="text-xs text-gray-500">
            Vigência: {tabela.vigenciaInicio} a {tabela.vigenciaFim}
          </p>
        </div>
        <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-700">
          {STATUS_LABEL[tabela.status]}
        </span>
      </div>

      {tabelaConfirmada ? (
        <TabelaPrecoExportButtons
          onExportarCsv={onExportarCsv}
          onExportarPdf={onExportarPdf}
        />
      ) : (
        <TabelaPrecoConfirmarLoteBar
          pendentes={pendentes}
          onConfirmarEmLote={onConfirmarEmLote}
          onConfirmarTabela={onConfirmarTabela}
        />
      )}

      <div className="space-y-2">
        {tabela.itens.map((item) => (
          <TabelaPrecoItemRow
            key={item.id}
            item={item}
            products={products}
            loadingProducts={loadingProducts}
            onConfirmar={(code) => onConfirmarItem(item.id, code)}
            onSemCorrespondencia={() => onSemCorrespondencia(item.id)}
          />
        ))}
      </div>
    </div>
  );
}
