"use client";

import React, { useEffect, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { useRouter } from "next/navigation";

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
  const { hasRole, isAuthenticated } = useAuth();
  const router = useRouter();
  const [hasVerified, setHasVerified] = useState(false);

  // Converte a role para array se for uma string
  const roleArray = Array.isArray(roles) ? roles : [roles];

  // Verifica se o usuário tem pelo menos uma das roles necessárias
  const hasPermission = roleArray.some((role) => hasRole(role));

  useEffect(() => {
    // Apenas verificar permissões se o usuário estiver autenticado
    if (isAuthenticated && !hasPermission && !ignoreRedirect) {
      // Se não tiver permissão e não for para ignorar o redirecionamento, redireciona
      router.push(redirectTo);
    }
    setHasVerified(true);
  }, [hasPermission, ignoreRedirect, isAuthenticated, redirectTo, router]);

  // Não mostrar nada enquanto verifica as permissões, exceto no caso de elementos de UI
  if (!hasVerified && !ignoreRedirect) {
    return null;
  }

  // Se for para ignorar o redirecionamento (componentes de UI), apenas renderiza ou não
  if (ignoreRedirect) {
    return hasPermission ? <>{children}</> : <>{fallback}</>;
  }

  // Nos outros casos, renderiza apenas se tiver permissão
  return hasPermission ? <>{children}</> : null;
}
