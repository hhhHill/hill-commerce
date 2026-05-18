import Link from "next/link";

import { LogoutButton } from "@/components/logout-button";
import { CategoryDirectory } from "@/features/storefront/catalog/category-list";
import { ProductCard } from "@/features/storefront/catalog/product-card";
import { StorefrontProductList } from "@/features/storefront/catalog/product-list";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { getSessionUser } from "@/lib/auth/server";
import {
  getServerStorefrontCategories,
  getServerStorefrontProducts,
  getServerStorefrontRecommendations
} from "@/lib/storefront/server";

export default async function HomePage() {
  const user = await getSessionUser();
  const [categories, products, recommendations] = await Promise.all([
    getServerStorefrontCategories(),
    getServerStorefrontProducts({ pageSize: 12 }),
    getServerStorefrontRecommendations({ type: "home", n: 10 })
  ]);

  return (
    <main className="min-h-screen bg-[var(--bg-page)] pb-24">
      <section className="sticky top-0 z-40 border-b border-[var(--border-normal)] bg-white/95 px-3 py-3 backdrop-blur md:px-4">
        <div className="mx-auto flex max-w-[1200px] flex-col gap-2">
          <div className="flex items-center gap-3">
            <Link className="shrink-0 text-lg font-extrabold text-[#ff4400]" href="/">
              Hill 购
            </Link>
            <SearchForm className="min-w-0 flex-1" />
          </div>
          <AccountQuickLinks user={user} />
        </div>
      </section>

      <div className="mx-auto flex max-w-[1200px] flex-col gap-3 px-3 py-3 md:grid md:grid-cols-[156px_minmax(0,1fr)] md:gap-4 md:px-4">
        <CategoryDirectory categories={categories} />

        <section className="flex min-w-0 flex-col gap-3">
          <section className="overflow-hidden rounded-lg bg-[linear-gradient(135deg,#ff7a18_0%,#ff4400_58%,#ff6200_100%)] px-4 py-4 text-white shadow-[0_8px_22px_rgba(255,80,0,0.18)] md:px-6 md:py-5">
            <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
              <div>
                <p className="text-sm font-semibold opacity-90">百亿补贴 · 今日爆款</p>
                <h1 className="mt-1 text-2xl font-extrabold tracking-normal md:text-3xl">低价好物，越逛越划算</h1>
                <p className="mt-2 text-sm opacity-90">限时会场、官方好货、包邮尖货都在这里。</p>
              </div>
              <Link className="inline-flex w-fit rounded-full bg-white px-4 py-2 text-sm font-bold text-[#ff4400]" href="/search">
                立即去逛
              </Link>
            </div>
          </section>

          <section className="grid grid-cols-3 gap-2.5">
            <OperationCard href="/search?keyword=%E9%99%90%E6%97%B6%E7%A7%92%E6%9D%80" kicker="限时秒杀" title="整点抢" value="低至9.9" />
            <OperationCard href="/search?keyword=%E7%99%BE%E4%BA%BF%E8%A1%A5%E8%B4%B4" kicker="百亿补贴" title="官方补" value="爆款直降" />
            <OperationCard href="/search?keyword=%E5%AE%98%E6%96%B9%E5%A5%BD%E8%B4%A7" kicker="官方好货" title="放心买" value="包邮精选" />
          </section>

          {recommendations.length > 0 ? (
            <section className="flex flex-col gap-3">
              <SectionTitle badge="HOT" title="百亿补贴" />
              <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:[grid-template-columns:repeat(auto-fill,minmax(150px,1fr))]">
                {recommendations.slice(0, 5).map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            </section>
          ) : null}

          <StorefrontProductList
            emptyDescription="当前还没有可展示的首页商品。"
            products={products.items}
            title="猜你喜欢"
            total={products.total}
          />
        </section>
      </div>
    </main>
  );
}

type AccountQuickLinksProps = {
  user: Awaited<ReturnType<typeof getSessionUser>>;
};

function AccountQuickLinks({ user }: AccountQuickLinksProps) {
  if (user) {
    return (
      <div className="flex flex-wrap items-center gap-3 text-xs text-[var(--text-secondary)] md:text-sm">
        <span>
          当前账号：<span className="font-semibold text-[var(--text-primary)]">{user.nickname}</span>
        </span>
        <Link className="text-[var(--brand-primary)]" href="/account">
          我的账户
        </Link>
        <Link className="text-[var(--brand-primary)]" href="/orders">
          我的订单
        </Link>
        <Link className="text-[var(--brand-primary)]" href="/account/addresses">
          收货地址
        </Link>
        <Link className="text-[var(--brand-primary)]" href="/admin">
          后台页
        </Link>
        <LogoutButton className="btn-secondary px-3 py-1.5 text-xs" />
      </div>
    );
  }

  return (
    <div className="flex flex-wrap items-center gap-3 text-xs text-[var(--text-secondary)] md:text-sm">
      <Link className="text-[var(--brand-primary)]" href="/login">
        登录
      </Link>
      <Link className="text-[var(--brand-primary)]" href="/register">
        注册
      </Link>
      <Link className="text-[var(--brand-primary)]" href="/search">
        全站搜索
      </Link>
    </div>
  );
}

type OperationCardProps = {
  href: string;
  kicker: string;
  title: string;
  value: string;
};

function OperationCard({ href, kicker, title, value }: OperationCardProps) {
  return (
    <Link className="rounded-lg border border-[#ffe0d2] bg-white px-3 py-3 shadow-[0_2px_8px_rgba(0,0,0,0.04)]" href={href}>
      <p className="text-[11px] font-bold text-[#ff4400]">{kicker}</p>
      <h2 className="mt-1 truncate text-sm font-extrabold text-[var(--text-primary)]">{title}</h2>
      <p className="mt-1 truncate text-xs text-[var(--text-secondary)]">{value}</p>
    </Link>
  );
}

function SectionTitle({ badge, title }: { badge: string; title: string }) {
  return (
    <div className="flex items-center gap-2">
      <span className="rounded-[4px] bg-[#ff4400] px-1.5 py-0.5 text-[10px] font-bold text-white">{badge}</span>
      <h2 className="text-lg font-extrabold text-[var(--text-primary)]">{title}</h2>
    </div>
  );
}
