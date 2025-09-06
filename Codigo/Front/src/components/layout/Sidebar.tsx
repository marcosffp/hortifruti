"use client";
import {
  BarChart,
  Bell,
  ChevronDown,
  Database,
  DollarSign,
  Home,
  type LucideIcon,
  ShoppingCart,
  Upload,
  Users,
} from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import RoleGuard from "@/components/auth/RoleGuard";

// Estrutura para submenu
interface MenuItem {
  label: string;
  icon: LucideIcon;
  href: string;
  submenu?: { label: string; icon: LucideIcon; href: string; roles?: string[] }[];
  roles?: string[];
}

const menu: MenuItem[] = [
  {
    label: "Módulo Financeiro",
    icon: DollarSign,
    href: "#",
    roles: ["MANAGER", "EMPLOYEE"],
    submenu: [
      { label: "Upload de Extratos", icon: Upload, href: "/financeiro/upload", roles: ["MANAGER"] },
      {
        label: "Lançamentos",
        icon: DollarSign,
        href: "/financeiro/lancamentos",
        roles: ["MANAGER", "EMPLOYEE"],
      },
      { label: "Fluxo de Caixa", icon: BarChart, href: "/financeiro/fluxo", roles: ["MANAGER", "EMPLOYEE"] },
    ],
  },
  {
    label: "Módulo Comércio",
    icon: ShoppingCart,
    href: "#",
    roles: ["MANAGER", "EMPLOYEE"],
    submenu: [
      { label: "Gestão de Clientes", icon: Users, href: "/comercio/clientes", roles: ["MANAGER", "EMPLOYEE"] },
    ],
  },
  { label: "Módulo Notificações", icon: Bell, href: "/notificacoes", roles: ["MANAGER", "EMPLOYEE"] },
  { label: "Módulo Backup", icon: Database, href: "/backup", roles: ["MANAGER"] },
  { label: "Módulo Relatórios", icon: BarChart, href: "/relatorios", roles: ["MANAGER"] },
  { label: "Módulo Acesso", icon: Users, href: "/acesso", roles: ["MANAGER"] },
  { label: "Administração", icon: Database, href: "/admin", roles: ["MANAGER"] },
];

export default function Sidebar() {
  const [openSubMenu, setOpenSubMenu] = useState<number | null>(null);

  const toggleSubMenu = (index: number) => {
    if (openSubMenu === index) {
      setOpenSubMenu(null);
    } else {
      setOpenSubMenu(index);
    }
  };

  return (
    <aside className="w-64 bg-white border-r min-h-screen p-4">
      <nav className="flex flex-col gap-1">
        {/* Home/Dashboard link */}
        <Link
          href="/"
          className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-green-100 text-gray-700"
        >
          <Home size={18} />
          <span>Dashboard</span>
        </Link>

        {/* Menu items with potential submenus */}
        {menu.map((item, i) => (
          <RoleGuard 
            key={`menu-item-${item.label}`} 
            roles={item.roles || []}
            ignoreRedirect={true}
          >
            <div className="w-full">
              {item.submenu ? (
                <div>
                  <button
                    type="button"
                    onClick={() => toggleSubMenu(i)}
                    className="flex items-center justify-between w-full px-3 py-2 rounded-lg hover:bg-green-100 text-gray-700"
                  >
                    <div className="flex items-center gap-2">
                      <item.icon size={18} />
                      <span>{item.label}</span>
                    </div>
                    <ChevronDown
                      size={16}
                      className={
                        openSubMenu === i
                          ? "transform rotate-180 transition-transform"
                          : "transition-transform"
                      }
                    />
                  </button>
                  {openSubMenu === i && (
                    <div className="ml-7 mt-1 border-l-2 border-green-200 pl-2">
                      {item.submenu.map((subItem) => (
                        <RoleGuard 
                          key={`submenu-${item.label}-${subItem.label}`}
                          roles={subItem.roles || []}
                          ignoreRedirect={true}
                        >
                          <Link
                            href={subItem.href}
                            className="flex items-center gap-2 px-3 py-2 text-sm rounded-lg hover:bg-green-50 text-gray-700"
                          >
                            <subItem.icon size={16} />
                            <span>{subItem.label}</span>
                          </Link>
                        </RoleGuard>
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <Link
                  href={item.href}
                  className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-green-100 text-gray-700"
                >
                  <item.icon size={18} />
                  <span>{item.label}</span>
                </Link>
              )}
            </div>
          </RoleGuard>
        ))}
      </nav>
    </aside>
  );
}
