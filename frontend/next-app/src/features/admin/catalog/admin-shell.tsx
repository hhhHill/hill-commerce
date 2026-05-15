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
  { href: "/admin/users", label: "用户管理", roles: ["ADMIN"] },
  { href: "/admin/dashboard", label: "仪表盘", roles: ["ADMIN"] }
];

export function AdminShell({ title, description, user, children }: AdminShellProps) {
  const navItems = NAV_ITEMS.filter((item) => !item.roles || item.roles.some((role) => user.roles.includes(role)));

  return (
    <main className="min-h-screen px-6 py-10">
      <section className="mx-auto flex max-w-7xl flex-col gap-6">
        <div className="rounded-[32px] border border-black/10 bg-[var(--surface)] p-8 shadow-[0_20px_60px_rgba(74,42,18,0.08)]">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div className="space-y-4">
              <span className="w-fit rounded-full bg-[var(--accent-strong)] px-4 py-1 text-sm font-semibold text-white">
                Sales Console
              </span>
              <div className="space-y-2">
                <h1 className="text-4xl font-bold tracking-tight">{title}</h1>
                <p className="max-w-3xl text-base leading-7 text-black/70">{description}</p>
              </div>
            </div>
            <div className="rounded-[24px] bg-white/85 p-5 text-sm shadow-[0_12px_30px_rgba(29,20,13,0.06)]">
              <p className="text-black/55">当前后台账号</p>
              <p className="mt-1 font-semibold">{user.nickname}</p>
              <p className="mt-1 text-black/65">{user.roles.join(", ")}</p>
              <div className="mt-4 flex flex-wrap gap-3">
                <Link className="rounded-full border border-black/10 px-4 py-2 font-medium" href="/">
                  返回首页
                </Link>
                <LogoutButton className="rounded-full bg-[var(--accent)] px-4 py-2 font-semibold text-white" />
              </div>
            </div>
          </div>
          <nav className="mt-6 flex flex-wrap gap-3">
            {navItems.map((item) => (
              <Link
                key={item.href}
                className="rounded-full border border-black/10 bg-white/70 px-4 py-2 text-sm font-medium transition hover:border-[var(--accent)] hover:text-[var(--accent)]"
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
