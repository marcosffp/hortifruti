import { NextRequest, NextResponse } from "next/server";

const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "";
// Só entra no connect-src se for uma URL absoluta — um caminho relativo (ex.:
// "/api", usado em produção para o proxy same-origin) já está coberto por
// 'self' e não é uma origem válida em CSP.
const apiOrigin = /^https?:\/\//.test(apiUrl) ? apiUrl : "";

export function proxy(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString("base64");

  const csp = [
    `default-src 'self'`,
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'`,
    `style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com`,
    `img-src 'self' data: blob:`,
    `font-src 'self' https://cdn.jsdelivr.net https://fonts.gstatic.com`,
    `connect-src 'self'${apiOrigin ? ` ${apiOrigin}` : ""}`,
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
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico|icon.png).*)",
  ],
};
