"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

type MobileBottomNavProps = {
  cartQuantity: number;
  isAuthenticated: boolean;
};

type NavItem = {
  href: string;
  label: string;
  match: (pathname: string) => boolean;
  badge?: number;
};

export function MobileBottomNav({ cartQuantity, isAuthenticated }: MobileBottomNavProps) {
  const pathname = usePathname();

  if (!pathname || pathname.startsWith("/admin") || pathname.startsWith("/api") || pathname.startsWith("/pay/")) {
    return null;
  }

  const navItems: NavItem[] = [
    { href: "/", label: "首页", match: (value) => value === "/" },
    { href: "/categories", label: "分类", match: (value) => value.startsWith("/categories") },
    { href: "/cart", label: "购物车", match: (value) => value.startsWith("/cart") || value.startsWith("/checkout"), badge: cartQuantity },
    { href: isAuthenticated ? "/account" : "/login", label: "我的", match: (value) => value.startsWith("/account") || value.startsWith("/orders") }
  ];

  const showCheckoutAction =
    pathname.startsWith("/products/") || pathname.startsWith("/cart") || pathname.startsWith("/checkout") || pathname.startsWith("/orders/");

  return (
    <div className="fixed inset-x-0 bottom-0 z-50 border-t border-[var(--border-normal)] bg-white/95 backdrop-blur md:hidden">
      <div className="mx-auto flex max-w-[1200px] items-center gap-2 px-3 py-2">
        <nav className="grid min-w-0 flex-1 grid-cols-4 gap-1">
          {navItems.map((item) => {
            const active = item.match(pathname);
            return (
              <Link
                key={item.href}
                className={`relative flex min-w-0 flex-col items-center justify-center rounded-2xl px-2 py-2 text-[11px] font-medium ${
                  active ? "bg-[#fff2ec] text-[var(--brand-primary)]" : "text-[var(--text-secondary)]"
                }`}
                href={item.href}
              >
                <span>{item.label}</span>
                {item.badge && item.badge > 0 ? (
                  <span className="absolute right-2 top-1 min-w-4 rounded-full bg-[var(--brand-primary)] px-1 text-center text-[10px] leading-4 text-white">
                    {item.badge > 99 ? "99+" : item.badge}
                  </span>
                ) : null}
              </Link>
            );
          })}
        </nav>
        {showCheckoutAction ? (
          <Link className="btn-primary shrink-0 px-4 py-3 text-xs" href={pathname.startsWith("/products/") ? "/cart" : "/checkout-summary"}>
            {pathname.startsWith("/products/") ? "去购物车" : "去结算"}
          </Link>
        ) : null}
      </div>
    </div>
  );
}
