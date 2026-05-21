import Link from "next/link";
import type { ReactNode } from "react";

import type { SessionUser } from "@/lib/auth/types";

type AnalyticsShellProps = {
  active: "overview" | "users" | "products";
  children: ReactNode;
  user: SessionUser;
};

export function AnalyticsShell({ active, children, user }: AnalyticsShellProps) {
  const isAdmin = user.roles.includes("ADMIN");
  const tabs = [
    { key: "overview", label: "概览", href: "/admin/analytics/overview" },
    ...(isAdmin ? [{ key: "users", label: "用户画像", href: "/admin/analytics/users" }] : []),
    { key: "products", label: "商品分析", href: "/admin/analytics/products" }
  ];

  return (
    <section className="space-y-6">
      <nav className="flex flex-wrap gap-3 rounded-[28px] border border-black/10 bg-white/90 p-3 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        {tabs.map((tab) => (
          <Link
            key={tab.key}
            className={`rounded-full px-4 py-2 text-sm font-semibold ${
              active === tab.key
                ? "bg-[var(--brand-primary)] text-white shadow-[0_8px_18px_rgba(255,77,30,0.18)]"
                : "border border-[var(--border-normal)] bg-white text-[var(--text-secondary)] hover:border-[var(--brand-primary)]"
            }`}
            href={tab.href}
          >
            {tab.label}
          </Link>
        ))}
      </nav>
      {children}
    </section>
  );
}
