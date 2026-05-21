"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { LogoutButton } from "@/components/logout-button";
import type { SessionUser, SessionUserRole } from "@/lib/auth/types";

type AdminSidebarProps = {
  user: SessionUser;
};

type NavItem = {
  href: string;
  label: string;
  roles?: SessionUserRole[];
};

const NAV_ITEMS: NavItem[] = [
  // MERCHANT & ADMIN shared
  { href: "/admin/shop", label: "我的店铺", roles: ["MERCHANT"] },
  { href: "/admin/products", label: "商品管理" },
  { href: "/admin/orders", label: "订单管理" },
  { href: "/admin/analytics/overview", label: "数据分析", roles: ["ADMIN", "MERCHANT"] },
  { href: "/admin/logs", label: "日志中心" },
  // ADMIN only
  { href: "/admin/dashboard", label: "仪表盘", roles: ["ADMIN"] },
  { href: "/admin/categories", label: "分类管理", roles: ["ADMIN"] },
  { href: "/admin/users", label: "用户管理", roles: ["ADMIN"] },
  { href: "/admin/shops", label: "店铺管理", roles: ["ADMIN"] },
];

export function AdminSidebar({ user }: AdminSidebarProps) {
  const pathname = usePathname();
  const navItems = NAV_ITEMS.filter(
    (item) => !item.roles || item.roles.some((role) => user.roles.includes(role)),
  );

  return (
    <aside className="flex w-[240px] shrink-0 flex-col border-r border-[var(--border-normal)] bg-white">
      <nav className="flex-1 overflow-auto py-2">
        {navItems.map((item) => {
          const isActive = pathname.startsWith(item.href);
          return (
            <Link
              className={`flex items-center px-5 py-2.5 text-sm transition-colors ${
                isActive
                  ? "bg-[#fff2ec] font-semibold text-[var(--brand-primary)]"
                  : "text-[var(--text-secondary)] hover:bg-[var(--bg-page)]"
              }`}
              href={item.href}
              key={item.href}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>
      <div className="border-t border-[var(--border-normal)] px-5 py-4">
        <p className="text-sm font-medium text-[var(--text-primary)]">{user.nickname}</p>
        <p className="text-xs text-[var(--text-hint)]">{user.roles.join(", ")}</p>
        <div className="mt-3 flex items-center gap-3">
          <Link
            className="text-xs text-[var(--brand-primary)] hover:underline"
            href="/"
          >
            返回首页
          </Link>
          <LogoutButton className="text-xs text-[var(--text-hint)] hover:text-red-600 transition-colors" />
        </div>
      </div>
    </aside>
  );
}
