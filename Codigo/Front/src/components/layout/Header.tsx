'use client';

import { Bell, User, LogOut, MenuIcon } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";

export default function Header({ onMenuClick }: { onMenuClick?: () => void }) {
  const { userName, logout } = useAuth();

  return (
    <header className="w-full bg-primary border-b border-[var(--neutral-300)]">
      <div className="flex justify-between items-center px-6 py-4 max-md:flex-col max-md:gap-3">
        {/* Left side - Logo and name */}
        <div className="flex items-center gap-3">
          {/* Logo */}
          <Image
            src="/icon.png"
            alt="Logo Hortifruti"
            width={50}
            height={50}
            className="h-12 w-12"
          />
          {/* Nome */}
          <div className="flex flex-col">
            <span className="text-lg font-bold text-white leading-tight">
              HORTIFRUTI
            </span>
            <span className="text-sm font-medium text-gray-200 -mt-1">
              SANTA LUZIA
            </span>
          </div>
        </div>
        {/* Right side - Phone, notifications, user, login */}
        <div className="flex items-center gap-4">
          <div className="menu md:hidden" onClick={onMenuClick} title="Menu">
            <MenuIcon className="text-gray-100 cursor-pointer " size={25} />
          </div>
          <span className="text-gray-100 text-sm">(31) 3649-7064</span>
          {/* Notification icon with badge */}
          <div className="relative" title="Notificações">
            <Bell className="text-gray-100" size={20} />
            <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full h-4 w-4 flex items-center justify-center">
              4
            </span>
          </div>
          {/* User avatar and name */}
          <div className="flex items-center gap-2" title="Perfil">
            <Link href="/perfil" className="flex items-center gap-2">
              <div className="w-8 h-8 bg-green-800 rounded-full flex items-center justify-center">
                <User className="text-white" size={16} />
              </div>
              <span className="text-gray-100 text-sm font-medium">{userName}</span>
            </Link>
          </div>
          <button
            type="button"
            onClick={logout}
            className="flex items-center gap-1 text-white font-semibold hover:text-red-700 transition-colors"
          >
            <LogOut size={16} />
            Sair
          </button>
        </div>
      </div>
    </header>
  );
}
