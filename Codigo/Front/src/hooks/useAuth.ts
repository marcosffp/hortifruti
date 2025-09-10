'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { authService } from '@/services/authService';

export function useAuth() {
  const router = useRouter();
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [userName, setUserName] = useState<string>('');
  const [userRoles, setUserRoles] = useState<string[]>([]);

  useEffect(() => {
    // Verificar autenticação quando o componente é montado
    checkAuth();
  }, []);

  const checkAuth = () => {
    const authenticated = authService.isAuthenticated();
    setIsAuthenticated(authenticated);

    if (authenticated) {
      const userInfo = authService.getUserInfo();
      setUserName(userInfo?.name || '');
      setUserRoles(userInfo?.roles || []);
    }

    setIsLoading(false);
  };

  const login = async (username: string, password: string) => {
    setIsLoading(true);
    try {
      await authService.login({ username, password });
      checkAuth();
      return true;
    } catch (error) {
      console.error('Erro ao fazer login:', error);
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    authService.logout();
    setIsAuthenticated(false);
    setUserName('');
    setUserRoles([]);
    router.push('/login');
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
    hasRole
  };
}
