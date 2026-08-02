"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { publicPages } from "@/config/publicPages";
import { authService, TransientAuthCheckError } from "@/services/authService";
import {
  getLastRefreshedAt,
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
        if (!(error instanceof TransientAuthCheckError)) throw error;
        // Não deu pra confirmar a sessão agora (rate limit/rede) — mantém o estado atual em vez de
        // deslogar; a checagem roda de novo na próxima navegação.
        setIsAuthChecked(true);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [pathname, router]);

  useEffect(() => {
    if (!isAuthenticated) return;

    const maybeRefresh = () => {
      const elapsed = Date.now() - getLastRefreshedAt();
      if (elapsed < SILENT_REFRESH_INTERVAL_MS) return;
      // Evita duas abas chamando /auth/refresh ao mesmo tempo: o refresh token é de uso único e o
      // backend revoga todas as sessões do usuário se detectar reuso de um token já rotacionado.
      if (!tryAcquireRefreshLock()) return;
      authService.refresh().catch(() => {
        // Falha silenciosa (rate limit/rede) — próximo tick ou o interceptor reativo tentam de novo.
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
