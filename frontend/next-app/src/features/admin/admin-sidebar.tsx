"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useState } from "react";

import { LogoutButton } from "@/components/logout-button";
import type { SessionUser, SessionUserRole } from "@/lib/auth/types";

type AdminSidebarProps = {
  user: SessionUser;
};

type NavChild = {
  href: string;
  label: string;
};

type NavItem = {
  href?: string;
  label: string;
  roles?: SessionUserRole[];
  children?: NavChild[];
};

const ORDER_CHILDREN: NavChild[] = [
  { href: "/admin/orders", label: "订单总览" },
  { href: "/admin/orders?status=PENDING_PAYMENT", label: "待支付" },
  { href: "/admin/orders?status=PAID", label: "未发货" },
  { href: "/admin/orders?status=SHIPPED", label: "已发货" },
  { href: "/admin/orders?status=COMPLETED", label: "已完成" },
  { href: "/admin/orders?status=CANCELLED", label: "已取消" },
  { href: "/admin/orders?status=CLOSED", label: "已关闭" },
];

const NAV_ITEMS: NavItem[] = [
  { href: "/admin/shop", label: "我的店铺", roles: ["MERCHANT"] },
  { href: "/admin/homepage", label: "首页运营", roles: ["ADMIN"] },
  { href: "/admin/products", label: "商品管理" },
  { label: "订单管理", children: ORDER_CHILDREN },
  { href: "/admin/analytics/overview", label: "数据分析", roles: ["ADMIN", "MERCHANT"] },
  { href: "/admin/product-logs", label: "商品日志" },
  { href: "/admin/dashboard", label: "仪表盘", roles: ["ADMIN"] },
  { href: "/admin/users", label: "用户管理", roles: ["ADMIN"] },
  { href: "/admin/shops", label: "店铺管理", roles: ["ADMIN"] },
];

export function AdminSidebar({ user }: AdminSidebarProps) {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const currentStatus = searchParams.get("status");

  const [expanded, setExpanded] = useState<string | null>(() => {
    if (pathname.startsWith("/admin/orders")) return "订单管理";
    return null;
  });

  const navItems = NAV_ITEMS.filter(
    (item) => !item.roles || item.roles.some((role) => user.roles.includes(role)),
  );

  function isParentActive(item: NavItem): boolean {
    if (item.href) return pathname.startsWith(item.href);
    return item.children?.some((c) => pathname === c.href.split("?")[0]) ?? false;
  }

  function isChildActive(child: NavChild): boolean {
    const [childPath, childQuery] = child.href.split("?");
    if (pathname !== childPath) return false;
    if (!childQuery) return !currentStatus;
    const childStatus = new URLSearchParams(childQuery).get("status");
    return childStatus === currentStatus;
  }

  return (
    <aside className="flex w-[240px] shrink-0 flex-col border-r border-[var(--border-normal)] bg-white">
      <nav className="flex-1 overflow-auto py-2">
        {navItems.map((item) => {
          if (item.children) {
            const active = isParentActive(item);
            const isOpen = expanded === item.label;

            return (
              <div key={item.label}>
                <button
                  className={`flex w-full items-center justify-between px-5 py-2.5 text-sm transition-colors ${
                    active && !isOpen
                      ? "bg-[#fff2ec] font-semibold text-[var(--brand-primary)]"
                      : "text-[var(--text-secondary)] hover:bg-[var(--bg-page)]"
                  }`}
                  onClick={() => setExpanded(isOpen ? null : item.label)}
                >
                  <span>{item.label}</span>
                  <svg
                    className={`h-3 w-3 shrink-0 transition-transform ${isOpen ? "rotate-90" : ""}`}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                </button>
                {isOpen && (
                  <div className="border-b border-[#f0f0f0] pb-1">
                    {item.children.map((child) => {
                      const childActive = isChildActive(child);
                      return (
                        <Link
                          key={child.href}
                          className={`flex items-center py-2 pl-10 pr-5 text-sm transition-colors ${
                            childActive
                              ? "bg-[#fff2ec] font-semibold text-[var(--brand-primary)]"
                              : "text-[var(--text-secondary)] hover:bg-[var(--bg-page)]"
                          }`}
                          href={child.href}
                        >
                          {child.label}
                        </Link>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          }

          const active = isParentActive(item);
          return (
            <Link
              key={item.href}
              className={`flex items-center px-5 py-2.5 text-sm transition-colors ${
                active
                  ? "bg-[#fff2ec] font-semibold text-[var(--brand-primary)]"
                  : "text-[var(--text-secondary)] hover:bg-[var(--bg-page)]"
              }`}
              href={item.href!}
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
          <Link className="text-xs text-[var(--brand-primary)] hover:underline" href="/">
            返回首页
          </Link>
          <LogoutButton className="text-xs text-[var(--text-hint)] hover:text-red-600 transition-colors" />
        </div>
      </div>
    </aside>
  );
}
