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

export interface DateInterval {
  start: string;
  end: string;
}

export function getWeekInterval(): DateInterval {
  const today = new Date();
  const lastMonday = new Date(today);
  lastMonday.setDate(today.getDate() - ((today.getDay() + 6) % 7) - 7);
  lastMonday.setHours(0, 0, 0, 0);

  const lastSaturday = new Date(lastMonday);
  lastSaturday.setDate(lastMonday.getDate() + 6);
  lastSaturday.setHours(23, 59, 59, 999);

  return {
    start: lastMonday.toISOString().split("T")[0],
    end: lastSaturday.toISOString().split("T")[0],
  };
}

export function getLastMonthInterval(): DateInterval {
  const today = new Date();
  const year =
    today.getMonth() === 0 ? today.getFullYear() - 1 : today.getFullYear();
  const month = today.getMonth() === 0 ? 11 : today.getMonth() - 1;

  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);

  return {
    start: firstDay.toISOString().split("T")[0],
    end: lastDay.toISOString().split("T")[0],
  };
}
