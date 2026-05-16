import Link from "next/link";
import type { ReactNode } from "react";

import { LogoutButton } from "@/components/logout-button";
import type { SessionUser, SessionUserRole } from "@/lib/auth/types";

type AdminShellProps = {
  title: string;
  description: string;
  user: SessionUser;
  children: ReactNode;
};

type NavItem = {
  href: string;
  label: string;
  roles?: SessionUserRole[];
};

const NAV_ITEMS: NavItem[] = [
  { href: "/admin", label: "后台总览" },
  { href: "/admin/categories", label: "分类管理" },
  { href: "/admin/products", label: "商品管理" },
  { href: "/admin/orders", label: "订单管理" },
  { href: "/admin/logs", label: "日志中心" },
  { href: "/admin/users", label: "用户管理", roles: ["ADMIN"] },
  { href: "/admin/dashboard", label: "仪表盘", roles: ["ADMIN"] },
  { href: "/admin/analytics/overview", label: "数据分析", roles: ["ADMIN", "SALES"] }
];

export function AdminShell({ title, description, user, children }: AdminShellProps) {
  const navItems = NAV_ITEMS.filter((item) => !item.roles || item.roles.some((role) => user.roles.includes(role)));

  return (
    <main className="min-h-screen bg-[var(--bg-admin)] px-4 py-4 md:px-6 md:py-6">
      <section className="mx-auto flex max-w-7xl flex-col gap-6">
        <div className="rounded-lg border border-[var(--border-normal)] bg-[var(--bg-admin)] p-6 shadow-[0_2px_8px_rgba(0,0,0,0.04)]">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div className="space-y-4">
              <span className="chip-badge w-fit">
                Sales Console
              </span>
              <div className="space-y-2">
                <h1 className="text-4xl font-bold tracking-tight">{title}</h1>
                <p className="max-w-3xl text-base leading-7 text-[var(--text-secondary)]">{description}</p>
              </div>
            </div>
            <div className="rounded-lg border border-[var(--border-normal)] bg-white p-4 text-sm shadow-[0_2px_8px_rgba(0,0,0,0.04)]">
              <p className="text-[var(--text-secondary)]">当前后台账号</p>
              <p className="mt-1 font-semibold">{user.nickname}</p>
              <p className="mt-1 text-[var(--text-secondary)]">{user.roles.join(", ")}</p>
              <div className="mt-4 flex flex-wrap gap-3">
                <Link className="btn-secondary px-4 py-2" href="/">
                  返回首页
                </Link>
                <LogoutButton className="btn-primary px-4 py-2" />
              </div>
            </div>
          </div>
          <nav className="mt-6 flex flex-wrap gap-3">
            {navItems.map((item) => (
              <Link
                key={item.href}
                className="rounded-full border border-[var(--border-normal)] bg-white px-4 py-2 text-sm font-medium transition hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={item.href}
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </div>
        {children}
      </section>
    </main>
  );
}
