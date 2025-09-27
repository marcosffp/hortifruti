"use client";

import { useEffect } from "react";
import { authService } from "@/services/authService";
import { useRouter } from "next/navigation";
import Loading from "@/components/ui/Loading";

export default function App() {
  const router = useRouter();

  useEffect(() => {
    if (authService.isAuthenticated()) {
      router.push("/dashboard");
    } else {
      router.push("/landing");
    }
  }, [router]);

  // Loading while checking authentication
  return (
    <div className="flex items-center justify-center min-h-screen">
      <Loading />
    </div>
  );
}
