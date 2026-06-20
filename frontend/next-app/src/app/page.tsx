import Link from "next/link";

import { CategoryDirectory } from "@/features/storefront/catalog/category-list";
import { loadHomepageData } from "@/features/storefront/catalog/homepage-data";
import { StorefrontProductList } from "@/features/storefront/catalog/product-list";
import { getSessionUser } from "@/lib/auth/server";

const DEFAULT_ACTIVITY_CARDS = [
  { title: "限时秒杀", imageUrl: null, linkUrl: "/search?keyword=限时秒杀" },
  { title: "百亿补贴", imageUrl: null, linkUrl: "/search?keyword=百亿补贴" },
  { title: "官方好货", imageUrl: null, linkUrl: "/search?keyword=官方好货" },
  { title: "新品首发", imageUrl: null, linkUrl: "/search?keyword=新品首发" }
];

export default async function HomePage() {
  const user = await getSessionUser();
  const { categories, products, recommendations, activityCards } = await loadHomepageData();

  return (
    <main className="min-h-screen bg-[var(--bg-page)] pb-24">
      {/* Top: three-column layout - columns stretch to equal height */}
      <div className="mx-auto flex max-w-[1600px] gap-3 px-2 py-3">
        {/* Left sidebar: category directory */}
        <aside className="hidden w-[260px] shrink-0 md:block">
          <CategoryDirectory categories={categories} />
        </aside>

        {/* Center: activity cards - fill to match category height */}
        <div className="flex min-w-0 flex-1">
          <div className="grid w-full grid-cols-2 grid-rows-2 gap-3">
            {(activityCards.length > 0 ? activityCards : DEFAULT_ACTIVITY_CARDS).map((card) => (
              <Link
                className="group relative overflow-hidden rounded-xl bg-[var(--bg-section)]"
                href={card.linkUrl}
                key={card.title}
              >
                {card.imageUrl ? (
                  <img alt={card.title} className="h-full w-full object-cover" src={card.imageUrl} />
                ) : (
                  <div className="flex h-full items-center justify-center">
                    <span className="text-lg font-bold text-[var(--text-primary)]">{card.title}</span>
                  </div>
                )}
              </Link>
            ))}
          </div>
        </div>

        {/* Right sidebar: user panel - fill to match category height */}
        <aside className="hidden w-[260px] shrink-0 lg:block">
          <UserPanel user={user} />
        </aside>
      </div>

      {/* Product grid - personalized when logged in, popular otherwise */}
      <div className="mx-auto max-w-[1600px] px-2 pb-3">
        <StorefrontProductList
          emptyDescription="当前还没有可展示的首页商品。"
          products={recommendations.length > 0 ? recommendations : products.items}
          total={recommendations.length > 0 ? recommendations.length : products.total}
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
