import type { NextConfig } from "next";

const nextConfig: NextConfig = {
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
