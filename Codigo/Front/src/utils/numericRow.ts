export type NumericField = "quantity" | "price" | "total";

export const NUMERIC_FIELDS: NumericField[] = ["quantity", "price", "total"];

export interface NumericRow {
  quantity: number;
  price: number;
  total: number;
  lastEdited: NumericField[];
}

export function round(value: number, decimals: number): number {
  const factor = 10 ** decimals;
  return Math.round(value * factor) / factor;
}

// Recalcula o 3º campo numérico (quantity/price/total) a partir dos outros dois assim que o
// usuário edita 2 deles — o campo que ficou "em aberto" (não editado por último) é o derivado.
export function recalcNumericRow<T extends NumericRow>(
  row: T,
  field: NumericField,
  value: number,
): T {
  const next: T = { ...row, [field]: value };
  next.lastEdited = [...row.lastEdited.filter((f) => f !== field), field].slice(
    -2,
  );

  if (next.lastEdited.length === 2) {
    const target = NUMERIC_FIELDS.find((f) => !next.lastEdited.includes(f));
    if (target === "total") {
      next.total = round(next.quantity * next.price, 2);
    } else if (target === "price") {
      next.price =
        next.quantity !== 0 ? round(next.total / next.quantity, 2) : 0;
    } else if (target === "quantity") {
      next.quantity = next.price !== 0 ? round(next.total / next.price, 3) : 0;
    }
  }

  return next;
}
