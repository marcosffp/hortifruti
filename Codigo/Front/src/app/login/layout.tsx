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
    <div
      className="relative min-h-screen flex flex-col overflow-hidden"
      style={{
        background: "#d9f7e1ff",
      }}
    >
      <div className="relative z-10 flex-1">{children}</div>
    </div>
  );
}
