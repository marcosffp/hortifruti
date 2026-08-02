"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { publicPages } from "@/config/publicPages";
import { authService } from "@/services/authService";
import {
  getLastRefreshAttemptedAt,
  markRefreshAttempted,
  tryAcquireRefreshLock,
} from "@/utils/authRefreshCoordinator";

const SILENT_REFRESH_INTERVAL_MS = 15 * 60 * 1000;
// Checagem mais frequente que o intervalo de renovação: em abas em segundo plano o navegador pode
// atrasar/pausar timers, então precisamos de um tick curto para recuperar rápido quando a aba volta
// a ficar visível, em vez de depender só do setInterval de 15min rodar pontualmente.
const REFRESH_CHECK_INTERVAL_MS = 60 * 1000;

export default function AuthGuard({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const pathname = usePathname();
  const [isAuthChecked, setIsAuthChecked] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      const isPublicPage = publicPages.includes(pathname);

      try {
        let user = await authService.me();

        if (!user && !isPublicPage) {
          const refreshed = await authService.refresh();
          if (refreshed) {
            user = await authService.me();
          }
        }

        const authenticated = !!user;

        if (cancelled) return;

        if (!authenticated && !isPublicPage) {
          router.push("/login");
        }

        if (authenticated && pathname === "/login") {
          router.push("/");
        }

        setIsAuthenticated(authenticated);
        setIsAuthChecked(true);
      } catch (error) {
        if (cancelled) return;
        // Não deu pra confirmar a sessão agora (rate limit, rede, resposta abortada por uma
        // navegação concorrente) — mantém o estado atual em vez de deslogar; a checagem roda de
        // novo na próxima navegação.
        console.error("Falha ao verificar sessão:", error);
        setIsAuthChecked(true);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [pathname, router]);

  useEffect(() => {
    if (!isAuthenticated) return;

    // Fica true assim que uma tentativa falha de verdade (403 — refresh token inválido), em vez de
    // um erro transitório (429/rede). Nesse caso o token está morto e não vai se recuperar sozinho
    // até um novo login, então paramos de bater no /auth/refresh a cada tick — só retoma quando
    // esse efeito remontar (ex.: troca de sessão via navegação).
    let refreshConfirmedDead = false;

    const maybeRefresh = () => {
      if (refreshConfirmedDead) return;

      // Usa o horário da última *tentativa* (sucesso ou falha), não só de sucesso — senão uma
      // falha nunca atualiza o relógio e a checagem seguinte (1 min depois) acha que já passou da
      // hora e tenta de novo imediatamente, virando um loop de tentativa a cada minuto.
      const elapsed = Date.now() - getLastRefreshAttemptedAt();
      if (elapsed < SILENT_REFRESH_INTERVAL_MS) return;
      // Evita duas abas chamando /auth/refresh ao mesmo tempo: o refresh token é de uso único e o
      // backend revoga todas as sessões do usuário se detectar reuso de um token já rotacionado.
      if (!tryAcquireRefreshLock()) return;

      markRefreshAttempted();
      authService
        .refresh()
        .then((refreshed) => {
          if (!refreshed) refreshConfirmedDead = true;
        })
        .catch(() => {
          // 429/rede — transitório, tenta de novo no próximo ciclo.
        });
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") maybeRefresh();
    };

    const intervalId = setInterval(maybeRefresh, REFRESH_CHECK_INTERVAL_MS);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      clearInterval(intervalId);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [isAuthenticated]);

  if (!isAuthChecked && !publicPages.includes(pathname)) {
    return null;
  }

  return <>{children}</>;
}
