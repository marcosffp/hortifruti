"use client";

import { usePathname, useRouter } from "next/navigation";
import type React from "react";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import { publicPages } from "@/config/publicPages";
import { authService, LoginError } from "@/services/authService";

export interface LoginResult {
  success: boolean;
  message?: string;
  retryAfter?: number;
}

interface AuthContextValue {
  isAuthenticated: boolean;
  isLoading: boolean;
  userName: string;
  userRoles: string[];
  environment: string;
  login: (username: string, password: string) => Promise<LoginResult>;
  logout: () => Promise<void>;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [userName, setUserName] = useState<string>("");
  const [userRoles, setUserRoles] = useState<string[]>([]);
  const [environment, setEnvironment] = useState<string>("");

  const checkAuth = useCallback(async () => {
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

    setIsLoading(false);
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

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        isLoading,
        userName,
        userRoles,
        environment,
        login,
        logout,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth deve ser usado dentro de um AuthProvider");
  }
  return context;
}
