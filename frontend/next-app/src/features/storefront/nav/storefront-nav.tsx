"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { SearchForm } from "../catalog/search-form";

type StorefrontNavProps = {
  user: { nickname: string } | null;
  cartQuantity: number;
};

const NAV_ITEMS = [
  { name: "首页", href: "/" },
  { name: "手机数码", href: "/search?keyword=%E6%89%8B%E6%9C%BA%E6%95%B0%E7%A0%81" },
  { name: "家用电器", href: "/search?keyword=%E5%AE%B6%E7%94%A8%E7%94%B5%E5%99%A8" },
  { name: "服饰鞋包", href: "/search?keyword=%E6%9C%8D%E9%A5%B0%E9%9E%8B%E5%8C%85" },
  { name: "美妆个护", href: "/search?keyword=%E7%BE%8E%E5%A6%86%E4%B8%AA%E6%8A%A4" },
  { name: "家居生活", href: "/search?keyword=%E5%AE%B6%E5%B1%85%E7%94%9F%E6%B4%BB" },
  { name: "食品饮料", href: "/search?keyword=%E9%A3%9F%E5%93%81%E9%A5%AE%E6%96%99" },
  { name: "母婴玩具", href: "/search?keyword=%E6%AF%8D%E5%A9%B4%E7%8E%A9%E5%85%B7" },
  { name: "运动户外", href: "/search?keyword=%E8%BF%90%E5%8A%A8%E6%88%B7%E5%A4%96" },
  { name: "汽车用品", href: "/search?keyword=%E6%B1%BD%E8%BD%A6%E7%94%A8%E5%93%81" }
];

export function StorefrontNav({ user, cartQuantity }: StorefrontNavProps) {
  const pathname = usePathname();

  if (!pathname || pathname.startsWith("/admin") || pathname.startsWith("/api") || pathname.startsWith("/pay/")) {
    return null;
  }

  return (
    <header>
      {/* Top bar: logo + search + user links — desktop */}
      <div className="hidden border-b border-[var(--border-normal)] bg-white/95 backdrop-blur md:block md:sticky md:top-0 md:z-40">
        <div className="mx-auto max-w-[1600px] px-2 py-3">
          <div className="flex gap-3">
            {/* Left: logo area — 200px, mirrors sidebar width */}
            <div className="hidden w-[200px] shrink-0 md:flex md:justify-center">
              <Link href="/">
                <img alt="Hill Commerce" className="h-12 w-auto" src="/images/logo_with_slogan.png" />
              </Link>
            </div>

            {/* Logo for md screens (no sidebar on md) — smaller */}
            <div className="hidden shrink-0 sm:block md:hidden">
              <Link href="/">
                <img alt="Hill Commerce" className="h-10 w-auto" src="/images/logo_with_slogan.png" />
              </Link>
            </div>

            {/* Center: search — same width as content column below */}
            <div className="min-w-0 flex-1">
              <SearchForm />
            </div>

            {/* Right: user links — 200px on lg+, compact on md */}
            <div className="hidden w-[200px] shrink-0 items-center justify-end gap-4 text-xs lg:flex">
              {user ? (
                <>
                  <span className="text-[var(--text-secondary)]">
                    Hi, <span className="font-semibold text-[var(--text-primary)]">{user.nickname}</span>
                  </span>
                  <Link className="nav-link" href="/account">我的账户</Link>
                  <Link className="nav-link" href="/orders">我的订单</Link>
                  <Link className="nav-link relative" href="/cart">
                    购物车
                    {cartQuantity > 0 ? (
                      <span className="absolute -right-3 -top-2 flex h-4 min-w-4 items-center justify-center rounded-full bg-[var(--brand-primary)] px-1 text-[10px] font-bold text-white">
                        {cartQuantity > 99 ? "99+" : cartQuantity}
                      </span>
                    ) : null}
                  </Link>
                </>
              ) : (
                <>
                  <Link className="nav-link" href="/login">登录</Link>
                  <Link className="nav-link" href="/register">注册</Link>
                  <Link className="nav-link relative" href="/cart">
                    购物车
                    {cartQuantity > 0 ? (
                      <span className="absolute -right-3 -top-2 flex h-4 min-w-4 items-center justify-center rounded-full bg-[var(--brand-primary)] px-1 text-[10px] font-bold text-white">
                        {cartQuantity > 99 ? "99+" : cartQuantity}
                      </span>
                    ) : null}
                  </Link>
                </>
              )}
            </div>

            {/* Right: compact links for md screens */}
            <div className="hidden shrink-0 items-center gap-3 text-xs md:flex lg:hidden">
              <Link className="nav-link" href={user ? "/account" : "/login"}>
                {user ? "账户" : "登录"}
              </Link>
              <Link className="nav-link relative" href="/cart">
                购物车
                {cartQuantity > 0 ? (
                  <span className="absolute -right-3 -top-2 flex h-4 min-w-4 items-center justify-center rounded-full bg-[var(--brand-primary)] px-1 text-[10px] font-bold text-white">
                    {cartQuantity > 99 ? "99+" : cartQuantity}
                  </span>
                ) : null}
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Category nav — desktop: aligns with center column */}
      <nav className="hidden border-b border-[var(--border-normal)] bg-white md:block">
        <div className="mx-auto flex max-w-[1600px] px-2">
          {/* Spacer matching left sidebar width */}
          <div className="hidden w-[200px] shrink-0 md:block" />
          <div className="flex min-w-0 flex-1 gap-0">
            {NAV_ITEMS.map((item) => {
              const isActive =
                item.href === "/" ? pathname === "/" : pathname.startsWith(item.href.split("?")[0] ?? "");
              return (
                <Link
                  className={`px-4 py-2.5 text-sm font-bold transition-colors ${
                    isActive ? "text-[var(--brand-primary)]" : "text-[var(--text-primary)] hover:text-[var(--brand-primary)]"
                  }`}
                  href={item.href}
                  key={item.name}
                >
                  {item.name}
                </Link>
              );
            })}
          </div>
        </div>
      </nav>

      {/* Mobile top bar */}
      <div className="sticky top-0 z-40 border-b border-[var(--border-normal)] bg-white/95 px-3 py-3 backdrop-blur md:hidden">
        <div className="flex items-center gap-3">
          <Link className="shrink-0 text-lg font-extrabold text-[var(--brand-primary)]" href="/">
            Hill 购
          </Link>
          <SearchForm className="min-w-0 flex-1" />
        </div>
      </div>
    </header>
  );
}
