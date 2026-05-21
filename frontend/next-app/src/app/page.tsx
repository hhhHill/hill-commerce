import Link from "next/link";

import { CategoryDirectory } from "@/features/storefront/catalog/category-list";
import { StorefrontProductList } from "@/features/storefront/catalog/product-list";
import { RecommendationSection } from "@/features/storefront/catalog/recommendation-section";
import { getSessionUser } from "@/lib/auth/server";
import {
  getServerStorefrontCategories,
  getServerStorefrontProducts,
  getServerStorefrontRecommendations,
} from "@/lib/storefront/server";

const ACTIVITY_CARDS = [
  { href: "/search?keyword=%E9%99%90%E6%97%B6%E7%A7%92%E6%9D%80", label: "限时秒杀", image: "" },
  { href: "/search?keyword=%E7%99%BE%E4%BA%BF%E8%A1%A5%E8%B4%B4", label: "百亿补贴", image: "" },
  { href: "/search?keyword=%E5%AE%98%E6%96%B9%E5%A5%BD%E8%B4%A7", label: "官方好货", image: "" },
  { href: "/search?keyword=%E6%96%B0%E5%93%81%E9%A6%96%E5%8F%91", label: "新品首发", image: "" },
];

export default async function HomePage() {
  const user = await getSessionUser();
  const [categories, products, recommendations] = await Promise.all([
    getServerStorefrontCategories(),
    getServerStorefrontProducts({ pageSize: 24 }),
    getServerStorefrontRecommendations({ type: "home", n: 10 }),
  ]);

  return (
    <main className="min-h-screen bg-[var(--bg-page)] pb-24">
      {/* Top: three-column layout — columns stretch to equal height */}
      <div className="mx-auto flex max-w-[1600px] gap-3 px-2 py-3">
        {/* Left sidebar: category directory */}
        <aside className="hidden w-[260px] shrink-0 md:block">
          <CategoryDirectory categories={categories} />
        </aside>

        {/* Center: activity cards — fill to match category height */}
        <div className="flex min-w-0 flex-1">
          <div className="grid grid-cols-2 grid-rows-2 gap-3 w-full">
            {ACTIVITY_CARDS.map((card) => (
              <Link
                className="group relative overflow-hidden rounded-xl bg-[var(--bg-section)]"
                href={card.href}
                key={card.label}
              >
                {card.image ? (
                  <img alt={card.label} className="h-full w-full object-cover" src={card.image} />
                ) : (
                  <div className="flex h-full items-center justify-center">
                    <span className="text-lg font-bold text-[var(--text-primary)]">
                      {card.label}
                    </span>
                  </div>
                )}
              </Link>
            ))}
          </div>
        </div>

        {/* Right sidebar: user panel — fill to match category height */}
        <aside className="hidden w-[260px] shrink-0 lg:block">
          <UserPanel user={user} />
        </aside>
      </div>

      {/* Recommendations */}
      {recommendations.length > 0 && (
        <section className="mx-auto max-w-[1600px] px-2 pb-3">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-bold text-[var(--text-primary)]">为你推荐</h2>
          </div>
          <RecommendationSection products={recommendations} />
        </section>
      )}

      {/* Bottom: full-width product grid */}
      <div className="mx-auto max-w-[1600px] px-2 pb-3">
        <StorefrontProductList
          emptyDescription="当前还没有可展示的首页商品。"
          products={products.items}
          total={products.total}
        />
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
      <div className="flex h-full flex-col overflow-hidden rounded-xl bg-white">
        <div className="border-b border-[var(--border-normal)] px-4 py-4">
          <p className="text-xs text-[var(--text-secondary)]">欢迎回来</p>
          <p className="mt-1 truncate text-sm font-bold text-[var(--text-primary)]">{user.nickname}</p>
        </div>
        <nav className="flex flex-1 flex-col">
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
    <div className="flex h-full flex-col overflow-hidden rounded-xl bg-white">
      <div className="border-b border-[var(--border-normal)] px-4 py-4">
        <p className="text-xs text-[var(--text-hint)]">你好</p>
        <Link className="mt-1 block text-sm font-bold text-[var(--brand-primary)]" href="/login">
          登录 / 注册
        </Link>
      </div>
      <nav className="flex flex-1 flex-col">
        <PanelLink href="/cart">购物车</PanelLink>
        <PanelLink href="/search">全站搜索</PanelLink>
      </nav>
    </div>
  );
}

function PanelLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link
      className="flex flex-1 items-center border-b border-[var(--border-light)] px-4 text-xs text-[var(--text-secondary)] transition-colors hover:bg-[#fff2ec] hover:text-[var(--brand-primary)]"
      href={href}
    >
      {children}
    </Link>
  );
}
