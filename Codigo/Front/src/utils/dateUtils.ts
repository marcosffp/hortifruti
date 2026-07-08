const SAO_PAULO_TZ = "America/Sao_Paulo";

// Formata uma data no fuso de Brasília como YYYY-MM-DD, independente do fuso do dispositivo do usuário
export function toSaoPauloDateString(date: Date = new Date()): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: SAO_PAULO_TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

export function todaySaoPaulo(): string {
  return toSaoPauloDateString(new Date());
}
