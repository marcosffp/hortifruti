"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { publicPages } from "@/config/publicPages";
import { useAuth } from "@/contexts/AuthContext";

/**
 * Gate de render + redirecionamento para as páginas protegidas do `(shell)`. Não faz mais sua
 * própria checagem de sessão (`/auth/me`/`/auth/refresh`) — isso vinha duplicado com
 * `AuthContext.checkAuth`, com dois estados (`isAuthenticated`) independentes que podiam divergir
 * entre si (ver auditoria de sessão/autenticação, achado F3). Aqui só se lê o resultado já
 * calculado pelo `AuthContext`, a única fonte de verdade agora.
 *
 * O gate de render depende de `isAuthenticated`, não só de "checagem concluída" (`isLoading`) —
 * antes disso era um bug real (achado F4, já documentado como item B-V1 no AUDITORIA.md): como
 * este componente vive no layout persistente do `(shell)` (não remonta em navegação interna), o
 * estado de "checagem concluída" da rota anterior sobrevivia por um instante durante a checagem da
 * rota nova, e o conteúdo protegido chegava a renderizar com sessão desatualizada. Como
 * `AuthContext.checkAuth` agora reseta `isLoading` para `true` a cada troca de rota (e todos os
 * refreshes silenciosos/reativos passam pelo mesmo estado central), essa janela deixa de existir.
 */
export default function AuthGuard({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const pathname = usePathname();
  const { isAuthenticated, isLoading } = useAuth();
  const isPublicPage = publicPages.includes(pathname);

  useEffect(() => {
    if (isLoading) return;

    if (!isAuthenticated && !isPublicPage) {
      router.push("/login");
    }
    if (isAuthenticated && pathname === "/login") {
      router.push("/");
    }
  }, [isAuthenticated, isLoading, isPublicPage, pathname, router]);

  if (isLoading && !isPublicPage) {
    return null;
  }

  if (!isAuthenticated && !isPublicPage) {
    return null;
  }

  return <>{children}</>;
}
