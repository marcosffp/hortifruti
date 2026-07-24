import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // O proxy de rewrites do Next mata a conexão em 30s por padrão (http-proxy
  // interno, ver proxy-request.js). Relatórios como /transactions/export-complete
  // consultam APIs fiscais externas lentas e passam bem disso, então a resposta
  // do backend nunca chega ao navegador (broken pipe) mesmo quando é gerada com
  // sucesso. 5 min cobre os relatórios mais lentos com folga.
  experimental: {
    proxyTimeout: 300_000,
  },
  // Proxeia /api/* para o backend (server-to-server). Do ponto de vista do
  // navegador as chamadas em NEXT_PUBLIC_API_URL (ex.: /api/auth) são same-origin,
  // então o cookie httpOnly do login vira first-party — evita o bloqueio de
  // cookies cross-site que navegadores como o Safari/iOS aplicam por padrão.
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${process.env.BACKEND_URL}/:path*`,
      },
    ];
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          {
            key: "Strict-Transport-Security",
            value: "max-age=63072000; includeSubDomains; preload",
          },
          {
            key: "X-Content-Type-Options",
            value: "nosniff",
          },
          {
            key: "X-Frame-Options",
            value: "DENY",
          },
          {
            key: "Referrer-Policy",
            value: "strict-origin-when-cross-origin",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
