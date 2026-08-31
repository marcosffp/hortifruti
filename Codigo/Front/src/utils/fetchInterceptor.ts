import { API_BASE_URL } from "@/config/api";
import { authService } from "@/services/authService";

const AUTH_BYPASS_PATHS = [
  "/auth",
  "/auth/refresh",
  "/auth/logout",
  "/auth/me",
];

/**
 * Atraso entre tentativas quando o próprio `fetch` lança uma exceção de rede (conexão recusada,
 * timeout, DNS — ex.: backend do Railway ainda acordando de uma hibernação). Só se aplica a GET:
 * repetir automaticamente uma escrita (POST/PUT/PATCH/DELETE) que já pode ter chegado ao servidor
 * arriscaria duplicar o efeito colateral (ex.: criar a mesma compra duas vezes) — para essas, o
 * erro sobe normalmente e quem chamou decide.
 */
const NETWORK_RETRY_DELAYS_MS = [500, 1500];

let installed = false;
let redirecting = false;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function resolveUrl(input: RequestInfo | URL): string {
  if (typeof input === "string") return input;
  if (input instanceof URL) return input.toString();
  return input.url;
}

function isApiRequest(url: string): boolean {
  return url.startsWith(API_BASE_URL);
}

function isAuthBypassRequest(url: string): boolean {
  return AUTH_BYPASS_PATHS.some((path) => url === `${API_BASE_URL}${path}`);
}

function redirectToLogin() {
  if (redirecting || window.location.pathname === "/login") return;
  redirecting = true;
  window.location.href = "/login";
}

async function fetchWithNetworkRetry(
  originalFetch: typeof fetch,
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<Response> {
  const method = (init?.method ?? "GET").toUpperCase();
  const isRetryable = method === "GET";

  for (let attempt = 0; ; attempt++) {
    try {
      return await originalFetch(input, init);
    } catch (error) {
      if (!isRetryable || attempt >= NETWORK_RETRY_DELAYS_MS.length)
        throw error;
      await sleep(NETWORK_RETRY_DELAYS_MS[attempt]);
    }
  }
}

/**
 * Instala um interceptor global de `fetch` que:
 * - repete automaticamente uma requisição GET que falhou por erro de rede (conexão recusada,
 *   timeout — ver `fetchWithNetworkRetry`), sem precisar que cada tela implemente seu próprio
 *   retry;
 * - ao receber um 401 de uma chamada da API (token ausente/expirado/inválido — ver
 *   TokenException.getHttpStatus() no backend), tenta renovar a sessão via /auth/refresh e
 *   reexecuta a requisição original uma única vez. Se a renovação falhar de verdade (sessão
 *   realmente inválida), redireciona para /login; se falhar por indisponibilidade transitória do
 *   backend, não redireciona — devolve a resposta 401 original para quem chamou tratar como erro
 *   pontual, sem forçar logout por causa de um problema de conexão (ver auditoria de
 *   sessão/autenticação, achados F1/F2).
 *
 * Propositalmente não reage a 403: no backend, 403 é reservado para "autenticado, mas sem
 * permissão" (role incorreta, `AccessDeniedException`) e para bloqueio de origem forjada — nenhum
 * dos dois se resolve com um refresh de token, e tentar mesmo assim só gastaria uma rotação de
 * refresh token à toa a cada ação negada por role (ver auditoria de sessão/autenticação, achado
 * F2, para o porquê disso importar: cada rotação extra é mais uma chance de coincidir com outra
 * concorrente e acionar a proteção contra reuso do backend).
 */
export function installFetchInterceptor() {
  if (installed) return;
  installed = true;

  const originalFetch = window.fetch.bind(window);

  window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const response = await fetchWithNetworkRetry(originalFetch, input, init);

    const url = resolveUrl(input);
    if (
      !isApiRequest(url) ||
      isAuthBypassRequest(url) ||
      response.status !== 401
    ) {
      return response;
    }

    const result = await authService.refresh();
    if (result.status === "authenticated") {
      return fetchWithNetworkRetry(originalFetch, input, init);
    }
    if (result.status === "unauthenticated") {
      redirectToLogin();
    }
    // "unavailable": não é problema de sessão, só indisponibilidade momentânea do backend —
    // devolve a resposta original em vez de deslogar por causa disso.
    return response;
  };
}
