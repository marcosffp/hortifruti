import type { ScoreWithBilletInfo } from "./types";

export function formatDate(dateString: string | null): string {
  if (!dateString) return "Não definida";
  try {
    const datePart = dateString.split("T")[0];
    const [year, month, day] = datePart.split("-");
    return `${day}/${month}/${year}`;
  } catch {
    return dateString;
  }
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
}

export function getStatusColor(status: string): string {
  switch (status) {
    case "PAGO":
      return "bg-green-100 text-green-800";
    case "PENDENTE":
      return "bg-blue-100 text-blue-800";
    case "BAIXADO":
    case "CANCELADO":
    case "CANCELADO_BOLETO":
      return "bg-red-100 text-red-800";
    default:
      return "bg-blue-100 text-blue-800";
  }
}

export function getStatusLabel(status: string): string {
  switch (status) {
    case "PAID":
      return "PAGO";
    case "PENDING":
      return "PENDENTE";
    case "OVERDUE":
      return "VENCIDO";
    case "CANCELLED":
      return "CANCELADO";
    case "CANCELADO_BOLETO":
      return "BOLETO CANCELADO";
    default:
      return status;
  }
}

/** Verifica se o boleto está em aberto (não permite deletar) */
export function isBilletOpen(score: ScoreWithBilletInfo): boolean {
  if (!score.billetInfo) return false;
  const status = score.billetInfo.situacaoBoleto.toLowerCase();
  return status.includes("aberto") || status.includes("pendente");
}
