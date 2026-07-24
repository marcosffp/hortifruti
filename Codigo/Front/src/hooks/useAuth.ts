"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { authService, LoginError } from "@/services/authService";

export interface LoginResult {
  success: boolean;
  message?: string;
  retryAfter?: number;
}

export function useAuth() {
  const router = useRouter();
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [userName, setUserName] = useState<string>("");
  const [userRoles, setUserRoles] = useState<string[]>([]);

  const checkAuth = useCallback(async () => {
    const user = await authService.me();
    setIsAuthenticated(!!user);

    if (user) {
      setUserName(user.name || "");
      setUserRoles(user.roles || []);
    } else {
      setUserName("");
      setUserRoles([]);
    }

    setIsLoading(false);
  }, []);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  const login = async (
    username: string,
    password: string,
  ): Promise<LoginResult> => {
    setIsLoading(true);
    try {
      await authService.login({ username, password });
      await checkAuth();
      return { success: true };
    } catch (error) {
      console.error("Erro ao fazer login:", error);
      if (error instanceof LoginError) {
        return {
          success: false,
          message: error.message,
          retryAfter: error.retryAfter,
        };
      }
      return { success: false };
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    await authService.logout();
    setIsAuthenticated(false);
    setUserName("");
    setUserRoles([]);
    router.push("/login");
  };

  const hasRole = (role: string): boolean => {
    return userRoles.includes(role);
  };

  return {
    isAuthenticated,
    isLoading,
    userName,
    userRoles,
    login,
    logout,
    hasRole,
  };
}
