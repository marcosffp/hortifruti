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
        showSuccess("Login realizado com sucesso!");
        router.push("/");
      } else {
        showError("Falha no login. Verifique suas credenciais.");
      }
    } catch (error) {
      showError("Falha no login. Verifique suas credenciais.");
      console.error("Erro no login:", error);
    }
  };

  return (
    <div className="w-full max-w-lg m-auto px-4 sm:px-0">
      <div
        className="border rounded-xl p-6 sm:p-8 shadow-lg backdrop-blur-sm"
        style={{ background: "rgba(255,255,255,0.85)" }}
      >
        <Image
          className="mx-auto mb-4 h-14 w-14"
          src="/icon.png"
          alt="Logo Hortifruti"
          width={56}
          height={56}
          onClick={() => router.push("/")}
        />
        <h2 className="text-center text-xl font-medium text-[color:var(--primary)] mb-2">
          Acesse sua conta
        </h2>
        <p className="text-center text-sm text-gray-500 mb-6">
          Informe seus dados para entrar no sistema.
        </p>
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
                className="block w-full px-2 py-3 mt-1 bg-transparent border-b border-gray-300 focus:border-[var(--primary)] focus:outline-none text-gray-900 placeholder:text-gray-400 transition-all"
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
                className="block w-full px-2 py-3 mt-1 bg-transparent border-b border-gray-300 focus:border-[var(--primary)] focus:outline-none text-gray-900 placeholder:text-gray-400 transition-all"
              />
            </div>
          </div>
          <div>
            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3 rounded-full bg-[var(--primary)] text-white font-semibold shadow hover:bg-[var(--primary-dark)] transition-all"
            >
              {isLoading ? "Entrando..." : "Entrar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
