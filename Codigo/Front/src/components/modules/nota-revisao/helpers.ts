import type { ItemNotaExtraido } from "@/types/notaExtracaoType";
import type { RevisaoRow } from "./types";

// A "data lida" vem da OCR como texto solto (dd/mm/aaaa) — tenta converter pra ISO (o formato que
// <input type="date"> e o backend esperam); se não bater com esse formato, quem chama cai pro
// default de hoje em vez de mandar uma data inválida/vazia pro backend.
export function parseDataLidaParaIso(dataLida: string | null): string | null {
  const match = dataLida?.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
  if (!match) return null;
  const [, dia, mes, ano] = match;
  return `${ano}-${mes}-${dia}`;
}

// Mesma margem de tolerância usada no backend (NotaConsistenciaChecker) — arredondamento de
// centavos não deve acusar inconsistência à toa.
export const MARGEM_CONSISTENCIA = 0.05;

export function itemBate(row: RevisaoRow): boolean {
  return Math.abs(row.quantity * row.price - row.total) < MARGEM_CONSISTENCIA;
}

export function itemToRow(item: ItemNotaExtraido): RevisaoRow {
  return {
    produtoLido: item.produtoLido,
    unidadeLida: item.unidade,
    produtoSugerido: item.produtoSugerido,
    confianca: item.confianca,
    code: item.produtoSugerido?.codigo ?? "",
    quantity: item.quantidade ?? 0,
    price: item.precoUnitario ?? 0,
    total: item.total ?? 0,
    lastEdited: ["quantity", "price"],
  };
}
