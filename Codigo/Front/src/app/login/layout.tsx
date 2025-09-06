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
      className="flex flex-col h-screen"
      style={{
        background: "linear-gradient(135deg, #ffffff 0%, #9cffbf 55%, #ffffff 100%)",
      }}
    >
      {children}
    </div>
  );
}
