"use client";

import { useState } from "react";
import ProductAutocompleteField from "@/components/ui/ProductAutocompleteField";
import type { FiscalProductType } from "@/types/purchaseType";
import type { TabelaPrecoClienteItemResponse } from "@/types/tabelaPrecoClienteType";

const CONFIANCA_BADGE: Record<string, string> = {
  alta: "bg-green-100 text-green-800",
  media: "bg-yellow-100 text-yellow-800",
  baixa: "bg-red-100 text-red-800",
};

const STATUS_BADGE: Record<
  TabelaPrecoClienteItemResponse["statusMatch"],
  { label: string; className: string }
> = {
  SUGERIDO: {
    label: "Sugerido — precisa revisar",
    className: "bg-yellow-100 text-yellow-800",
  },
  CONFIRMADO: { label: "Confirmado", className: "bg-green-100 text-green-800" },
  EDITADO_MANUALMENTE: {
    label: "Editado manualmente",
    className: "bg-blue-100 text-blue-800",
  },
  SEM_CORRESPONDENCIA: {
    label: "Sem correspondência",
    className: "bg-red-100 text-red-800",
  },
};

function formatPreco(preco: number | null): string {
  if (preco == null) return "sem preço esse mês";
  return preco.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
}

interface TabelaPrecoItemRowProps {
  item: TabelaPrecoClienteItemResponse;
  products: FiscalProductType[];
  loadingProducts: boolean;
  readOnly: boolean;
  onConfirmar: (code: string) => Promise<void>;
  onSemCorrespondencia: () => Promise<void>;
}

export default function TabelaPrecoItemRow({
  item,
  products,
  loadingProducts,
  readOnly,
  onConfirmar,
  onSemCorrespondencia,
}: TabelaPrecoItemRowProps) {
  const [code, setCode] = useState(item.fiscalProductCodigo ?? "");
  const [salvando, setSalvando] = useState(false);
  const status = STATUS_BADGE[item.statusMatch];

  const confirmar = async () => {
    if (!code) return;
    setSalvando(true);
    try {
      await onConfirmar(code);
    } finally {
      setSalvando(false);
    }
  };

  const semCorrespondencia = async () => {
    setSalvando(true);
    try {
      await onSemCorrespondencia();
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div className="border border-gray-200 rounded-lg p-3 space-y-2">
      <p className="text-xs text-gray-500 flex items-center gap-2 flex-wrap">
        <span>
          Cliente:{" "}
          <span className="font-medium text-gray-700">
            {item.nomeProdutoCliente}
          </span>{" "}
          <span className="font-mono">({item.codigoProdutoCliente})</span>
        </span>
        <span
          className={`px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase ${status.className}`}
        >
          {status.label}
        </span>
        {item.confiancaMatching != null && (
          <span
            className={`px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase ${
              CONFIANCA_BADGE[item.confiancaMatching >= 0.85 ? "alta" : "media"]
            }`}
          >
            {Math.round(item.confiancaMatching * 100)}% match
          </span>
        )}
        <span className="text-gray-400">{formatPreco(item.preco)}</span>
      </p>
      {readOnly ? (
        <p className="text-sm text-gray-700">
          {item.fiscalProductCodigo
            ? `${item.fiscalProductCodigo} — ${item.fiscalProductDescricao}`
            : "sem produto vinculado"}
        </p>
      ) : (
        <div className="flex flex-col lg:flex-row lg:items-center gap-2">
          <div className="flex-1">
            <ProductAutocompleteField
              products={products}
              value={code}
              onSelect={setCode}
              disabled={loadingProducts || salvando}
            />
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={confirmar}
              disabled={!code || salvando}
              className="py-2 px-3 text-sm bg-primary text-white rounded disabled:opacity-50 cursor-pointer hover:bg-[var(--primary-dark)]"
            >
              Confirmar
            </button>
            <button
              type="button"
              onClick={semCorrespondencia}
              disabled={salvando}
              className="py-2 px-3 text-sm border border-gray-300 text-gray-700 rounded disabled:opacity-50 cursor-pointer hover:bg-gray-50"
            >
              Sem correspondência
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
