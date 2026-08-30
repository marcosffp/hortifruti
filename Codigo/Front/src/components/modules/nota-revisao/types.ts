import type { ProdutoSugerido } from "@/types/notaExtracaoType";
import type { NumericRow } from "@/utils/numericRow";

export interface RevisaoRow extends NumericRow {
  produtoLido: string;
  unidadeLida: string | null;
  produtoSugerido: ProdutoSugerido | null;
  confianca: "alta" | "media" | "baixa" | null;
  code: string;
  quantidadeKgConvertida: number | null;
  precoPorKgConvertido: number | null;
  conversaoEstimada: boolean | null;
}

export const CONFIANCA_BADGE: Record<"alta" | "media" | "baixa", string> = {
  alta: "bg-green-100 text-green-800",
  media: "bg-yellow-100 text-yellow-800",
  baixa: "bg-red-100 text-red-800",
};
