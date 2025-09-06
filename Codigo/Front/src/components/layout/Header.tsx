import { Bell, User } from "lucide-react";
import Image from "next/image";

export default function Header() {
  return (
    <header className="w-full bg-white border-b">
      <div className="flex justify-between items-center px-6 py-4">
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
            <span className="text-lg font-bold text-gray-900 leading-tight">
              HORTIFRUTI
            </span>
            <span className="text-sm font-medium text-gray-600 -mt-1">
              SANTA LUZIA
            </span>
          </div>
        </div>
        {/* Right side - Phone, notifications, user, login */}
        <div className="flex items-center gap-4">
          <span className="text-gray-700 text-sm">(31) 3649-7064</span>
          {/* Notification icon with badge */}
          <div className="relative">
            <Bell className="text-gray-500" size={20} />
            <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full h-4 w-4 flex items-center justify-center">
              4
            </span>
          </div>
          {/* User avatar and name */}
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-green-600 rounded-full flex items-center justify-center">
              <User className="text-white" size={16} />
            </div>
            <span className="text-gray-700 text-sm font-medium">Gestor</span>
          </div>
          <button
            type="button"
            className="text-green-600 font-semibold hover:text-green-700"
          >
            Entrar
          </button>
        </div>
      </div>
    </header>
  );
}
