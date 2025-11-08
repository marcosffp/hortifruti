import type { Metadata } from "next";
import "@/app/globals.css";

export const metadata: Metadata = {
  title: "Hortifruti Santa Luzia | Acesso",
  description:
    "Página de login do sistema Hortifruti Santa Luzia. Faça login para acessar o sistema de gestão",
};

export default function LoginLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <>
      <style
        dangerouslySetInnerHTML={{
          __html: `
          @keyframes blob {
            0% {
              transform: translate(0px, 0px) scale(1);
            }
            33% {
              transform: translate(30px, -50px) scale(1.1);
            }
            66% {
              transform: translate(-20px, 20px) scale(0.9);
            }
            100% {
              transform: translate(0px, 0px) scale(1);
            }
          }
          .animate-blob {
            animation: blob 7s infinite;
          }
          .animation-delay-2000 {
            animation-delay: 2s;
          }
          .animation-delay-4000 {
            animation-delay: 4s;
          }
        `,
        }}
      />
      <div
        className="relative min-h-screen flex flex-col overflow-hidden"
        style={{
          background: "#d9f7e1ff",
        }}
      >
        {/* Padrão de fundo animado (desabilitado para fundo branco) */}
        <div className="absolute inset-0 opacity-0 pointer-events-none">
          <div className="absolute top-0 left-0 w-96 h-96 bg-white rounded-full mix-blend-multiply filter blur-3xl animate-blob"></div>
          <div className="absolute top-0 right-0 w-96 h-96 bg-green-200 rounded-full mix-blend-multiply filter blur-3xl animate-blob animation-delay-2000"></div>
          <div className="absolute bottom-0 left-1/2 w-96 h-96 bg-emerald-300 rounded-full mix-blend-multiply filter blur-3xl animate-blob animation-delay-4000"></div>
        </div>

        {/* Grid decorativo (desabilitado para fundo branco) */}
        <div className="absolute inset-0 opacity-0 pointer-events-none">
          <div
        className="h-full w-full"
        style={{
          backgroundImage: `
            linear-gradient(rgba(255, 255, 255, 0.1) 1px, transparent 1px),
            linear-gradient(90deg, rgba(255, 255, 255, 0.1) 1px, transparent 1px)
          `,
          backgroundSize: "50px 50px",
        }}
          ></div>
        </div>

        {/* Conteúdo */}
        <div className="relative z-10 flex-1">{children}</div>
      </div>
    </>
  );
}
