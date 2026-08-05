import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Permite acessar o dev server por outro host além de localhost (ex.: testar do celular pelo
  // IP da rede local). Sem isso, o Next bloqueia o WebSocket de hot-reload por origem não
  // autorizada, o que trava a hidratação do React (a página carrega mas nada reage a clique).
  // Só afeta `next dev` — ignorado em build/produção. Atualize o IP se ele mudar (rede
  // diferente, DHCP renovado) — no Mac: `ipconfig getifaddr en0`.
  allowedDevOrigins: ["192.168.100.68"],
  // A compressão gzip embutida do Next precisa acumular um bloco inteiro antes de liberar
  // qualquer byte pro cliente — isso inclui as respostas proxeadas por `rewrites()` abaixo, como o
  // SSE de `/api/compras/notas/stream` (ver CapturaSseEmitterRegistry). Resultado: a conexão do
  // EventSource fica "aberta" no Network do DevTools, mas nenhum evento chega em tempo real, só
  // quando o buffer enche ou a conexão fecha — na prática, obriga a recarregar a página pra ver
  // qualquer atualização. Desligar a compressão aqui resolve; o Railway/proxy de produção já
  // comprime na frente do Next de qualquer forma.
  compress: false,
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
