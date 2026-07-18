"use client";

import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { authService } from "@/services/authService";

const publicPages = ["/login"];

export default function AuthGuard({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const pathname = usePathname();
  const [isAuthChecked, setIsAuthChecked] = useState(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      const isPublicPage = publicPages.includes(pathname);
      const user = await authService.me();
      const isAuthenticated = !!user;

      if (cancelled) return;

      if (!isAuthenticated && !isPublicPage) {
        router.push("/login");
      }

      if (isAuthenticated && pathname === "/login") {
        router.push("/");
      }

      setIsAuthChecked(true);
    })();

    return () => {
      cancelled = true;
    };
  }, [pathname, router]);

  if (!isAuthChecked && !publicPages.includes(pathname)) {
    return null;
  }

  return <>{children}</>;
}
