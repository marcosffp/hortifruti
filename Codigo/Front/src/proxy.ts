import { type NextRequest, NextResponse } from "next/server";

const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "";
// Só entra no connect-src se for uma URL absoluta — um caminho relativo (ex.:
// "/api", usado em produção para o proxy same-origin) já está coberto por
// 'self' e não é uma origem válida em CSP.
const apiOrigin = /^https?:\/\//.test(apiUrl) ? apiUrl : "";

// WebSocket de tempo real (ver useRealtimeSocket.ts) vai direto pro backend, nunca pelo proxy
// same-origin de `/api` — precisa da própria entrada no connect-src, senão o navegador bloqueia
// a conexão mesmo com o handshake sendo aceito pelo servidor.
const wsUrl = process.env.NEXT_PUBLIC_WS_URL ?? "";

export function proxy(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString("base64");
  // Em dev o React usa eval() para reconstruir stack traces; isso nunca
  // acontece em build de produção, então só relaxamos o CSP localmente.
  const isDev = process.env.NODE_ENV !== "production";

  const csp = [
    `default-src 'self'`,
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${isDev ? " 'unsafe-eval'" : ""}`,
    // Split em duas diretivas (CSP nível 3): 'unsafe-inline' fica restrito a
    // style="" em atributo (necessário pro `style` dinâmico do React e pro
    // Leaflet, que posiciona marcadores escrevendo em el.style via JS) — o
    // vetor mais perigoso, injeção de <style> inteiro via XSS, continua
    // bloqueado por style-src-elem não ter 'unsafe-inline'.
    `style-src-elem 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com`,
    `style-src-attr 'unsafe-inline'`,
    `img-src 'self' data: blob:`,
    `font-src 'self' https://cdn.jsdelivr.net https://fonts.gstatic.com`,
    `connect-src 'self' https://viacep.com.br${apiOrigin ? ` ${apiOrigin}` : ""}${wsUrl ? ` ${wsUrl}` : ""}`,
    `frame-src 'self' blob:`,
    `frame-ancestors 'none'`,
    `base-uri 'self'`,
    `form-action 'self'`,
  ].join("; ");

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set("Content-Security-Policy", csp);

  const response = NextResponse.next({
    request: { headers: requestHeaders },
  });
  response.headers.set("Content-Security-Policy", csp);

  return response;
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|icon.png).*)"],
};
