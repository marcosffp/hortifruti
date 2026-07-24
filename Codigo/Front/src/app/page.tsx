"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import Loading from "@/components/ui/Loading";
import { authService } from "@/services/authService";

export default function App() {
  const router = useRouter();

  useEffect(() => {
    (async () => {
      const user = await authService.me();
      if (user) {
        const isManager = user.roles?.includes("MANAGER");
        router.push(isManager ? "/dashboard" : "/comercio/compras");
      } else {
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
