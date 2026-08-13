export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

/**
 * Alguns controllers do backend (ex. `DispositivoController`, `NotaController`,
 * `NotificationController`, `RecommendationController`, o endpoint de backup do módulo
 * Acesso) já incluem "/api" no próprio `@RequestMapping`, diferente da maioria (ex.
 * `/clients`). Como `API_BASE_URL` é o prefixo do rewrite same-origin do Next (`/api`, ver
 * next.config.ts), a URL final precisa repetir "/api" pra virar `/api/api/...` no browser —
 * o rewrite descarta o primeiro `/api` e repassa `/api/...` pro backend, que é o que esses
 * controllers esperam. Não é bug — não "corrija" removendo o "/api" repetido.
 */
export const API_BASE_URL_WITH_API_PREFIX = `${API_BASE_URL}/api`;
