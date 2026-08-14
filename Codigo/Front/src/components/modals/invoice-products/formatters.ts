export function calculateTotal(price: number, quantity: number): number {
  return price * quantity;
}

export function formatQuantity(quantity: number | string): string {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(parseFloat(quantity.toString()));
}
