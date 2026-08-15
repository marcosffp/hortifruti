"use client";

import type React from "react";
import RoleGuard from "@/components/auth/RoleGuard";

interface GestorOnlyProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
  redirectTo?: string;
  ignoreRedirect?: boolean;
}

/** Atalho para `<RoleGuard roles={["MANAGER"]}>` — evita esquecer a role ao repetir a guarda numa mesma página. */
export default function GestorOnly({
  children,
  fallback,
  redirectTo,
  ignoreRedirect,
}: GestorOnlyProps) {
  return (
    <RoleGuard
      roles={["MANAGER"]}
      fallback={fallback}
      redirectTo={redirectTo}
      ignoreRedirect={ignoreRedirect}
    >
      {children}
    </RoleGuard>
  );
}
