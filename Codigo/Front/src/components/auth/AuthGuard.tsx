"use client";

import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { authService } from "@/services/authService";

// Páginas que não precisam de autenticação
const publicPages = ["/login"];

export default function AuthGuard({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const pathname = usePathname();
  const [isAuthChecked, setIsAuthChecked] = useState(false);

  useEffect(() => {
    // Verifica se é uma página pública
    const isPublicPage = publicPages.includes(pathname);

    // Verifica se o usuário está autenticado
    const isAuthenticated = authService.isAuthenticated();

    // Se não estiver autenticado e a página não for pública, redireciona para o login
    if (!isAuthenticated && !isPublicPage) {
      router.push("/login");
    }

    // Se estiver autenticado e tentar acessar o login, redireciona para a home
    if (isAuthenticated && pathname === "/login") {
      router.push("/");
    }

    setIsAuthChecked(true);
  }, [pathname, router]);

  // Enquanto verifica a autenticação, não renderiza nada
  if (!isAuthChecked && !publicPages.includes(pathname)) {
    return null;
  }

  return <>{children}</>;
}
