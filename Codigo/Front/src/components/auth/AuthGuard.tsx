"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { authService } from "@/services/authService";

const publicPages = ["/login"];
const SILENT_REFRESH_INTERVAL_MS = 15 * 60 * 1000;

export default function AuthGuard({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const pathname = usePathname();
  const [isAuthChecked, setIsAuthChecked] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      const isPublicPage = publicPages.includes(pathname);
      const user = await authService.me();
      const authenticated = !!user;

      if (cancelled) return;

      if (!authenticated && !isPublicPage) {
        router.push("/login");
      }

      if (authenticated && pathname === "/login") {
        router.push("/");
      }

      setIsAuthenticated(authenticated);
      setIsAuthChecked(true);
    })();

    return () => {
      cancelled = true;
    };
  }, [pathname, router]);

  useEffect(() => {
    if (!isAuthenticated) return;

    const intervalId = setInterval(() => {
      authService.refresh();
    }, SILENT_REFRESH_INTERVAL_MS);

    return () => clearInterval(intervalId);
  }, [isAuthenticated]);

  if (!isAuthChecked && !publicPages.includes(pathname)) {
    return null;
  }

  return <>{children}</>;
}
