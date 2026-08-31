"use client";

import { usePathname, useRouter } from "next/navigation";
import type React from "react";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { publicPages } from "@/config/publicPages";
import type { AuthUser } from "@/services/authService";
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

const SILENT_REFRESH_INTERVAL_MS = 15 * 60 * 1000;

/**
 * Atraso entre tentativas quando `/auth/me`/`/auth/refresh` respondem "unavailable" (5xx, 429,
 * erro de rede/timeout — ex.: cold start do Railway). Crescente e com poucas tentativas: o
 * suficiente para atravessar uma instabilidade curta sem martelar o backend nem prender o usuário
 * numa tela de carregamento por tempo demais — se todas as tentativas falharem, mantemos o último
 * estado de sessão conhecido (nunca deslogamos por indisponibilidade) e paramos de tentar até a
 * próxima navegação ou o próximo refresh silencioso reativar a checagem.
 */
const UNAVAILABLE_RETRY_DELAYS_MS = [2000, 5000, 10000];

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [userName, setUserName] = useState<string>("");
  const [userRoles, setUserRoles] = useState<string[]>([]);
  const [environment, setEnvironment] = useState<string>("");

  // Cada chamada de checkAuth ganha um número de geração; se uma checagem mais nova começar
  // (troca de rota) enquanto uma mais antiga ainda está no meio do laço de retry, a antiga se
  // reconhece como obsoleta e para de aplicar estado — sem isso, uma resposta atrasada de uma
  // checagem já abandonada poderia sobrescrever o resultado (correto) da checagem mais recente.
  const checkGeneration = useRef(0);

  const applyUser = useCallback((user: AuthUser | null) => {
    setIsAuthenticated(!!user);
    setUserName(user?.name ?? "");
    setUserRoles(user?.roles ?? []);
    setEnvironment(user?.environment ?? "");
  }, []);

  const checkAuth = useCallback(async () => {
    const myGeneration = ++checkGeneration.current;
    const isPublicPage = publicPages.includes(pathname);
    const isCurrent = () => checkGeneration.current === myGeneration;

    setIsLoading(true);

    for (let attempt = 0; ; attempt++) {
      const meResult = await authService.me();
      if (!isCurrent()) return;

      if (meResult.status === "authenticated") {
        applyUser(meResult.user);
        setIsLoading(false);
        return;
      }

      if (meResult.status === "unauthenticated") {
        if (isPublicPage) {
          applyUser(null);
          setIsLoading(false);
          return;
        }

        const refreshResult = await authService.refresh();
        if (!isCurrent()) return;

        if (refreshResult.status === "authenticated") {
          applyUser(refreshResult.user);
          setIsLoading(false);
          return;
        }

        if (refreshResult.status === "unauthenticated") {
          applyUser(null);
          setIsLoading(false);
          return;
        }

        // refresh "unavailable" -> cai no retry abaixo, igual a me() indisponível
      }

      if (attempt >= UNAVAILABLE_RETRY_DELAYS_MS.length) {
        // Desiste por agora sem mexer em isAuthenticated/userName/etc — indisponibilidade
        // transitória nunca deve ser tratada como logout (achado F1 da auditoria de sessão).
        setIsLoading(false);
        return;
      }
      await sleep(UNAVAILABLE_RETRY_DELAYS_MS[attempt]);
      if (!isCurrent()) return;
    }
  }, [pathname, applyUser]);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  // Refresh silencioso periódico enquanto autenticado: mantém o access token fresco durante uso
  // contínuo, sem depender só da renovação reativa em erro. O resultado é intencionalmente
  // ignorado aqui (fire-and-forget) — se a sessão realmente tiver morrido, a próxima navegação
  // (checkAuth acima) ou a próxima chamada de API (fetchInterceptor) vão notar e agir.
  useEffect(() => {
    if (!isAuthenticated) return;

    const intervalId = setInterval(() => {
      authService.refresh();
    }, SILENT_REFRESH_INTERVAL_MS);

    return () => clearInterval(intervalId);
  }, [isAuthenticated]);

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
