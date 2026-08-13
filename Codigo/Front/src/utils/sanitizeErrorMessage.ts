const MAX_MESSAGE_LENGTH = 300;

// Sinais de que o corpo devolvido pelo backend não é uma mensagem de erro pensada pra exibição
// (stack trace, dump de exception, HTML de página de erro) — nesses casos é mais seguro mostrar
// o fallback genérico do que repassar o texto cru pro usuário.
const SUSPICIOUS_PATTERNS = [
  /<\/?[a-z][\s\S]*>/i, // tags HTML
  /\bat\s+[\w.$]+\(/, // linha de stack trace Java/JS ("at com.foo.Bar(...")
  /Exception\b/,
  /Caused by:/,
];

export function sanitizeErrorMessage(
  raw: string | null | undefined,
  fallback: string,
): string {
  const trimmed = raw?.trim();
  if (!trimmed) return fallback;
  if (trimmed.length > MAX_MESSAGE_LENGTH) return fallback;
  if (SUSPICIOUS_PATTERNS.some((pattern) => pattern.test(trimmed))) {
    return fallback;
  }
  return trimmed;
}
