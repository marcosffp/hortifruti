"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import Loading from "@/components/ui/Loading";
import { authService, TransientAuthCheckError } from "@/services/authService";

export default function App() {
  const router = useRouter();

  useEffect(() => {
    (async () => {
      try {
        const user = await authService.me();
        if (user) {
          const isManager = user.roles?.includes("MANAGER");
          router.push(isManager ? "/dashboard" : "/comercio/compras");
        } else {
          router.push("/landing");
        }
      } catch (error) {
        if (!(error instanceof TransientAuthCheckError)) throw error;
        // Não deu pra confirmar a sessão agora (rate limit/rede) — manda pra landing como
        // fallback seguro em vez de propagar o erro; se o usuário estiver logado, a próxima
        // navegação autenticada confirma a sessão normalmente.
        router.push("/landing");
      }
    })();
  }, [router]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <Loading />
    </div>
  );
}
