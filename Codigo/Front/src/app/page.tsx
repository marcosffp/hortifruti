"use client";

import { useEffect } from "react";
import { authService } from "@/services/authService";
import { useRouter } from "next/navigation";

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
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-500"></div>
    </div>
  );
}
