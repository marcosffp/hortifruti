"use client";

import { useRouter } from "next/navigation";
import type React from "react";
import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";

interface RoleGuardProps {
  roles: string | string[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
  redirectTo?: string;
  ignoreRedirect?: boolean;
}

/**
 * Componente que renderiza o conteúdo apenas se o usuário tiver uma das roles especificadas
 * @param roles - Uma string única ou array de strings com as roles que podem acessar o conteúdo
 * @param children - O conteúdo a ser mostrado se o usuário tiver permissão
 * @param fallback - Conteúdo alternativo a ser mostrado se o usuário não tiver permissão (opcional)
 * @param redirectTo - Página para redirecionar se o usuário não tiver permissão (opcional, padrão: '/acesso-negado')
 * @param ignoreRedirect - Se true, não redireciona mesmo sem permissão (útil para elementos de UI)
 */
export default function RoleGuard({
  roles,
  children,
  fallback = null,
  redirectTo = "/acesso-negado",
  ignoreRedirect = false,
}: RoleGuardProps) {
  const { hasRole, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const [hasVerified, setHasVerified] = useState(false);

  const roleArray = Array.isArray(roles) ? roles : [roles];
  const hasPermission = roleArray.some((role) => hasRole(role));

  useEffect(() => {
    if (isLoading) return;
    if (isAuthenticated && !hasPermission && !ignoreRedirect) {
      router.push(redirectTo);
    }
    setHasVerified(true);
  }, [
    hasPermission,
    ignoreRedirect,
    isAuthenticated,
    isLoading,
    redirectTo,
    router,
  ]);

  if (isLoading || (!hasVerified && !ignoreRedirect)) {
    return null;
  }

  if (ignoreRedirect) {
    return hasPermission ? children : fallback;
  }

  return hasPermission ? children : null;
}
