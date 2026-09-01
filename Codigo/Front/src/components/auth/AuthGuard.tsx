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
 * estado de "checagem concluída" da rota anterior sobrevivia indefinidamente, e o conteúdo
 * protegido podia renderizar já com uma sessão sabidamente inválida (ex.: depois de um logout).
 *
 * `isLoading` (em `AuthContext`) só liga para a primeira checagem da vida do app — as rechecagens
 * disparadas por troca de rota rodam em segundo plano, sem apagar a tela a cada navegação (era
 * literalmente isso que acontecia numa primeira versão desta correção: cada clique piscava a tela
 * inteira em branco até `/auth/me` responder, mesmo com a sessão continuando válida o tempo todo).
 * O trade-off aceito: por uma fração de segundo a mais numa troca de rota, o conteúdo da página
 * nova pode renderizar com o `isAuthenticated`/`userRoles` de antes da rechecagem terminar — se ela
 * concluir que a sessão não é mais válida, `isAuthenticated` vira `false` e o efeito abaixo
 * redireciona pro login imediatamente. Isso é bem mais raro e bem menos grave do que apagar a UI
 * inteira em toda navegação, e as chamadas de API continuam protegidas de verdade pelo backend
 * (401/403) e pelo `fetchInterceptor`, independente do que a UI mostra nesse intervalo.
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
