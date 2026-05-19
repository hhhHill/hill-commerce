import Link from "next/link";

import { CategoryDirectory } from "@/features/storefront/catalog/category-list";
import { ProductCard } from "@/features/storefront/catalog/product-card";
import { StorefrontProductList } from "@/features/storefront/catalog/product-list";
import { getSessionUser } from "@/lib/auth/server";
import {
  getServerStorefrontCategories,
  getServerStorefrontProducts,
  getServerStorefrontRecommendations
} from "@/lib/storefront/server";

const OPERATION_LINKS = [
  { href: "/search?keyword=%E9%99%90%E6%97%B6%E7%A7%92%E6%9D%80", label: "限时秒杀" },
  { href: "/search?keyword=%E7%99%BE%E4%BA%BF%E8%A1%A5%E8%B4%B4", label: "百亿补贴" },
  { href: "/search?keyword=%E5%AE%98%E6%96%B9%E5%A5%BD%E8%B4%A7", label: "官方好货" }
];

export default async function HomePage() {
  const user = await getSessionUser();
  const [categories, products, recommendations] = await Promise.all([
    getServerStorefrontCategories(),
    getServerStorefrontProducts({ pageSize: 12 }),
    getServerStorefrontRecommendations({ type: "home", n: 10 })
  ]);

  return (
    <main className="min-h-screen bg-[var(--bg-page)] pb-24">
      <div className="mx-auto flex max-w-[1600px] gap-3 px-2 py-3">
        {/* Left sidebar: category directory — desktop only */}
        <aside className="hidden w-[200px] shrink-0 md:block md:sticky md:top-4 md:self-start">
          <CategoryDirectory categories={categories} />
        </aside>

        {/* Center content */}
        <div className="flex min-w-0 flex-1 flex-col gap-4">
          {/* Operations bar */}
          <div className="flex items-center gap-6 rounded-xl bg-[var(--bg-section)] px-4 py-3">
            <Link className="text-sm font-bold text-[var(--brand-primary)] hover:underline" href="/search">
              全站特卖
            </Link>
            {OPERATION_LINKS.map((link) => (
              <Link
                className="text-sm text-[var(--text-secondary)] transition-colors hover:text-[var(--brand-primary)]"
                href={link.href}
                key={link.label}
              >
                {link.label}
              </Link>
            ))}
            <span className="ml-auto text-xs text-[var(--text-hint)]">品质好货 · 全场包邮</span>
          </div>

          {/* Recommendations */}
          {recommendations.length > 0 ? (
            <section>
              <div className="mb-3 flex items-center gap-2">
                <span className="rounded-[2px] bg-[var(--brand-primary)] px-1.5 py-0.5 text-[10px] font-bold text-white">
                  HOT
                </span>
                <h2 className="text-lg font-bold text-[var(--text-primary)]">百亿补贴</h2>
              </div>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
                {recommendations.slice(0, 10).map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            </section>
          ) : null}

          {/* 猜你喜欢 */}
          <StorefrontProductList
            emptyDescription="当前还没有可展示的首页商品。"
            products={products.items}
            title="猜你喜欢"
            total={products.total}
          />
        </div>

        {/* Right sidebar: user panel — desktop only */}
        <aside className="hidden w-[200px] shrink-0 lg:block lg:sticky lg:top-4 lg:self-start">
          <UserPanel user={user} />
        </aside>
      </div>

      {/* Mobile category bar */}
      <div className="md:hidden">
        <CategoryDirectory categories={categories} />
      </div>
    </main>
  );
}

type UserPanelProps = {
  user: Awaited<ReturnType<typeof getSessionUser>>;
};

function UserPanel({ user }: UserPanelProps) {
  if (user) {
    return (
      <div className="overflow-hidden rounded-xl bg-white">
        <div className="border-b border-[var(--border-normal)] px-4 py-4">
          <p className="text-xs text-[var(--text-secondary)]">欢迎回来</p>
          <p className="mt-1 truncate text-sm font-bold text-[var(--text-primary)]">{user.nickname}</p>
        </div>
        <nav className="flex flex-col">
          <PanelLink href="/account">我的账户</PanelLink>
          <PanelLink href="/orders">我的订单</PanelLink>
          <PanelLink href="/cart">购物车</PanelLink>
          <PanelLink href="/account/addresses">收货地址</PanelLink>
          <PanelLink href="/search">全站搜索</PanelLink>
        </nav>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-xl bg-white">
      <div className="border-b border-[var(--border-normal)] px-4 py-4">
        <p className="text-xs text-[var(--text-hint)]">你好</p>
        <Link className="mt-1 block text-sm font-bold text-[var(--brand-primary)]" href="/login">
          登录 / 注册
        </Link>
      </div>
      <nav className="flex flex-col">
        <PanelLink href="/cart">购物车</PanelLink>
        <PanelLink href="/search">全站搜索</PanelLink>
      </nav>
    </div>
  );
}

function PanelLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link
      className="border-b border-[var(--border-light)] px-4 py-3 text-xs text-[var(--text-secondary)] transition-colors hover:bg-[#fff2ec] hover:text-[var(--brand-primary)]"
      href={href}
    >
      {children}
    </Link>
  );
}
