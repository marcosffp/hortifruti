"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { showError, showSuccess } from "@/services/notificationService";
import { useAuth } from "@/hooks/useAuth";

export default function Login() {
  const router = useRouter();
  const { login, isLoading } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      const success = await login(username, password);
      if (success) {
        showSuccess('Login realizado com sucesso!');
        router.push('/');
      } else {
        showError('Falha no login. Verifique suas credenciais.');
      }
    } catch (error) {
      showError('Falha no login. Verifique suas credenciais.');
      console.error('Erro no login:', error);
    }
  };

  return (
    <div className="w-full max-w-md m-auto px-4 sm:px-0">
      <div className="bg-gray-100 border rounded-md p-8">
        <Image
          className="mx-auto h-20 w-auto cursor-pointer transition-discrete duration-200 hover:scale-105"
          src="/icon.png"
          onClick={() => {
            router.push("/");
          }}
          alt={"Logo Hortifruti"}
          width={80}
          height={80}
        />
        <h2 className="mt-6 text-center text-3xl font-bold tracking-tight text-gray-900">
          Acesse sua conta
        </h2>
        <form className="space-y-6 mt-4" onSubmit={handleSubmit}>
          <div>
            <label
              htmlFor="username"
              className="block text-sm font-medium text-gray-700"
            >
              Usuário
            </label>
            <div className="mt-1">
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                placeholder="Digite seu nome de usuário"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="px-2 py-3 mt-1 block w-full rounded-md border border-gray-300 shadow-sm focus:border-[var(--primary-button-dark)] focus:outline-none focus:ring-[var(--primary-button-dark)] sm:text-sm"
              />
            </div>
          </div>
          <div>
            <label
              htmlFor="password"
              className="block text-sm font-medium text-gray-700"
            >
              Senha
            </label>
            <div className="mt-1">
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                placeholder="Digite sua senha"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="px-2 py-3 mt-1 block w-full rounded-md border border-gray-300 shadow-sm focus:border-[var(--primary-button-dark)] focus:outline-none focus:ring-[var(--primary-button-dark)] sm:text-sm"
              />
            </div>
          </div>
          <div>
            <button
              type="submit"
              disabled={isLoading}
              className="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-[var(--primary-button-dark)] hover:bg-[var(--primary-button-dark-hover)] cursor-pointer"
            >
              {isLoading ? 'Entrando...' : 'Entrar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
