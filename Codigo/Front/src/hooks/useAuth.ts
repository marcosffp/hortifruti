"use client";

import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { publicPages } from "@/config/publicPages";
import {
  authService,
  LoginError,
  TransientAuthCheckError,
} from "@/services/authService";

export interface LoginResult {
  success: boolean;
  message?: string;
  retryAfter?: number;
}

export function useAuth() {
  const router = useRouter();
  const pathname = usePathname();
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [userName, setUserName] = useState<string>("");
  const [userRoles, setUserRoles] = useState<string[]>([]);
  const [environment, setEnvironment] = useState<string>("");

  const checkAuth = useCallback(async () => {
    try {
      let user = await authService.me();

      // Na tela de login nunca existe sessão a renovar — sem essa checagem, todo
      // acesso a /login sem sessão disparava um refresh fadado a falhar com 403.
      if (!user && !publicPages.includes(pathname)) {
        const refreshed = await authService.refresh();
        if (refreshed) {
          user = await authService.me();
        }
      }

      setIsAuthenticated(!!user);

      if (user) {
        setUserName(user.name || "");
        setUserRoles(user.roles || []);
        setEnvironment(user.environment || "");
      } else {
        setUserName("");
        setUserRoles([]);
        setEnvironment("");
      }
    } catch (error) {
      if (!(error instanceof TransientAuthCheckError)) throw error;
      // Não deu pra confirmar a sessão agora (rate limit/rede) — mantém o estado atual em vez de
      // deslogar; a próxima checagem (troca de página) tenta de novo.
    } finally {
      setIsLoading(false);
    }
  }, [pathname]);

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
    setEnvironment("");
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
    environment,
    login,
    logout,
    hasRole,
  };
}
