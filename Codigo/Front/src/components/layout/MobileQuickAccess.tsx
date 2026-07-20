"use client";

import { Home } from "lucide-react";
import Link from "next/link";
import RoleGuard from "@/components/auth/RoleGuard";
import { menu } from "@/components/layout/Sidebar";

const allShortcuts = [
  { label: "Dashboard", icon: Home, href: "/dashboard", roles: [] as string[] },
  ...menu.flatMap((item) =>
    item.submenu
      ? item.submenu.map((sub) => ({
          label: sub.label,
          icon: sub.icon,
          href: sub.href,
          roles: sub.roles || [],
        }))
      : [
          {
            label: item.label,
            icon: item.icon,
            href: item.href,
            roles: item.roles || [],
          },
        ],
  ),
];

// Primeira linha do grid: os atalhos mais usados no dia a dia
const priorityHrefs = ["/comercio/boletos", "/comercio/compras", "/notificacoes"];

const shortcuts = [
  ...priorityHrefs
    .map((href) => allShortcuts.find((item) => item.href === href))
    .filter((item): item is (typeof allShortcuts)[number] => Boolean(item)),
  ...allShortcuts.filter((item) => !priorityHrefs.includes(item.href)),
];

export default function MobileQuickAccess() {
  return (
    <section className="md:hidden mb-6" aria-label="Acesso rápido">
      <h2 className="text-sm font-semibold text-gray-600 mb-3">
        O que você deseja fazer?
      </h2>
      <div className="grid grid-cols-3 gap-3">
        {shortcuts.map((item) => {
          const card = (
            <Link
              key={item.href}
              href={item.href}
              className="flex flex-col items-center justify-center gap-2 rounded-xl border border-gray-200 bg-white p-3 text-center shadow-sm active:bg-primary/10"
            >
              <span className="flex items-center justify-center w-12 h-12 rounded-full bg-primary/10 text-primary">
                <item.icon size={22} />
              </span>
              <span className="text-xs font-medium text-gray-700 leading-tight">
                {item.label}
              </span>
            </Link>
          );

          if (item.roles.length === 0) {
            return card;
          }

          return (
            <RoleGuard key={item.href} roles={item.roles} ignoreRedirect={true}>
              {card}
            </RoleGuard>
          );
        })}
      </div>
    </section>
  );
}
