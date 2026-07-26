"use client";

import { LogOut, MenuIcon, User } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";

export default function Header({ onMenuClick }: { onMenuClick?: () => void }) {
  const { userName, logout } = useAuth();

  return (
    <header className="w-full bg-primary border-b border-[var(--neutral-300)]">
      <div className="flex justify-between items-center px-6 py-4 max-md:flex-col max-md:gap-3">
        <div className="flex items-center gap-3">
          <Image
            src="/icon.png"
            alt="Logo Hortifruti"
            width={50}
            height={50}
            className="h-12 w-12"
          />
          <div className="flex flex-col">
            <span className="text-lg font-bold text-white leading-tight">
              HORTIFRUTI
            </span>
            <span className="text-sm font-medium text-gray-200 -mt-1">
              SANTA LUZIA
            </span>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <button
            type="button"
            className="menu md:hidden"
            onClick={onMenuClick}
            title="Menu"
            aria-label="Abrir menu"
          >
            <MenuIcon className="text-gray-100 cursor-pointer " size={25} />
          </button>
          <span className="text-gray-100 text-sm">(31) 3649-7064</span>
          <div className="flex items-center gap-2" title="Perfil">
            <Link
              href="/perfil"
              prefetch={false}
              className="flex items-center gap-2"
            >
              <div className="w-8 h-8 bg-green-800 rounded-full flex items-center justify-center">
                <User className="text-white" size={16} />
              </div>
              <span className="text-gray-100 text-sm font-medium">
                {userName}
              </span>
            </Link>
          </div>
          <button
            type="button"
            onClick={logout}
            className="flex items-center gap-1 text-white font-semibold hover:text-red-700 transition-colors cursor-pointer"
          >
            <LogOut size={16} />
            Sair
          </button>
        </div>
      </div>
    </header>
  );
}
