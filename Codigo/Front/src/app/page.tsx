"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import Loading from "@/components/ui/Loading";
import { useAuth } from "@/contexts/AuthContext";

export default function App() {
  const router = useRouter();
  const { isAuthenticated, isLoading, userRoles } = useAuth();

  useEffect(() => {
    if (isLoading) return;

    if (isAuthenticated) {
      const isManager = userRoles.includes("MANAGER");
      router.push(isManager ? "/dashboard" : "/comercio/compras");
    } else {
      router.push("/landing");
    }
  }, [isAuthenticated, isLoading, userRoles, router]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <Loading />
    </div>
  );
}
