"use client";

import Header from "@/components/layout/Header";
import Sidebar from "@/components/layout/Sidebar";
import AuthGuard from "@/components/auth/AuthGuard";
import { useState } from "react";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <AuthGuard>
      <div className="flex flex-col h-screen">
        <Header onMenuClick={() => setSidebarOpen(true)} />
        <div className="flex flex-1 overflow-hidden">
          <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

          <div
            className={`md:hidden fixed inset-0 bg-black/50 z-40 transition-opacity ${sidebarOpen ? "opacity-100" : "opacity-0 pointer-events-none"}`}
            onClick={() => setSidebarOpen(false)}
            aria-hidden={!sidebarOpen}
            role="button"
            tabIndex={sidebarOpen ? 0 : -1}
            aria-label="Fechar menu lateral"
          />

          <main
            className={`
              flex-1 overflow-y-auto 
              transform transition-transform duration-300 
              ${sidebarOpen ? "max-md:translate-x-64" : "max-md:translate-x-0"}
            `}
            role="main"
            aria-label="Conteúdo principal"
          >
            {children}
          </main>
        </div>
      </div>
    </AuthGuard>
  );
}
