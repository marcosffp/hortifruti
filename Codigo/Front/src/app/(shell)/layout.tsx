import type { Metadata } from "next";
import Header from "@/components/layout/Header";
import Sidebar from "@/components/layout/Sidebar";
import AuthGuard from "@/components/auth/AuthGuard";

export const metadata: Metadata = {
  title: "Hortifruti Santa Luzia",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <AuthGuard>
      <div className="flex flex-col h-screen">
        <Header />
        <div className="flex flex-1">
          <Sidebar />
          {children}
        </div>
      </div>
    </AuthGuard>
  );
}
