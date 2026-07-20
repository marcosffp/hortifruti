import { API_BASE_URL } from "@/config/api";
import { authService } from "@/services/authService";

const AUTH_BYPASS_PATHS = [
  "/auth",
  "/auth/refresh",
  "/auth/logout",
  "/auth/me",
];

let installed = false;
let redirecting = false;

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

/**
 * Instala um interceptor global de `fetch` que, ao receber um 403 de uma chamada da API (token
 * expirado), tenta renovar a sessão via /auth/refresh e reexecuta a requisição original uma única
 * vez. Se a renovação falhar, redireciona para /login em vez de deixar a tela travada com erros.
 */
export function installFetchInterceptor() {
  if (installed) return;
  installed = true;

  const originalFetch = window.fetch.bind(window);

  window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const response = await originalFetch(input, init);

    const url = resolveUrl(input);
    if (
      !isApiRequest(url) ||
      isAuthBypassRequest(url) ||
      response.status !== 403
    ) {
      return response;
    }

    const refreshed = await authService.refresh();
    if (!refreshed) {
      redirectToLogin();
      return response;
    }

    return originalFetch(input, init);
  };
}
