import type { Metadata } from "next";
import "@/app/globals.css";

export const metadata: Metadata = {
  title: "Hortifruti Santa Luzia | Vincular dispositivo",
  description:
    "Vincule este celular para fotografar notas de compra sem precisar fazer login.",
};

/**
 * Fora do route group `(shell)` de propósito: quem abre essa rota é o navegador do celular antes
 * de ter qualquer credencial (nem cookie de login, nem device token ainda) — se ficasse dentro do
 * `(shell)`, o `AuthGuard` chutaria a página pra `/login` antes mesmo do formulário aparecer.
 */
export default function DispositivoLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ background: "#d9f7e1ff" }}
    >
      {children}
    </div>
  );
}
