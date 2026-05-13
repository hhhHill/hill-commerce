import Link from "next/link";

import { LogoutButton } from "@/components/logout-button";
import { CategoryDirectory } from "@/features/storefront/catalog/category-list";
import { StorefrontProductList } from "@/features/storefront/catalog/product-list";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { getSessionUser } from "@/lib/auth/server";
import { getServerStorefrontCategories, getServerStorefrontProducts } from "@/lib/storefront/server";

export default async function HomePage() {
  const user = await getSessionUser();
  const [categories, products] = await Promise.all([
    getServerStorefrontCategories(),
    getServerStorefrontProducts({ pageSize: 6 })
  ]);

  return (
    <main className="min-h-screen px-6 py-10">
      <div className="mx-auto flex max-w-6xl flex-col gap-10">
        <section className="overflow-hidden rounded-[36px] border border-black/10 bg-[linear-gradient(145deg,rgba(255,250,242,0.94)_0%,rgba(232,209,182,0.96)_100%)] px-8 py-8 shadow-[0_24px_70px_rgba(74,42,18,0.08)] lg:px-10 lg:py-10">
          <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-end">
            <div className="flex flex-col gap-5">
              <span className="w-fit rounded-full bg-[var(--accent)] px-4 py-1 text-sm font-semibold text-white">hill-commerce</span>
              <div className="flex flex-col gap-4">
                <h1 className="max-w-3xl text-5xl font-semibold tracking-tight">从浏览到理解商品，这里就是前台链路的起点。</h1>
                <p className="max-w-2xl text-base leading-8 text-black/68">
                  先把访客能看到什么、能怎么找、详情能说明到什么程度做扎实，后续购物车、结算、下单才有真实入口可以承接。
                </p>
              </div>
              <div className="flex flex-wrap gap-3">
                <Link className="rounded-full bg-[var(--accent)] px-5 py-2 font-semibold text-white" href="/categories">
                  开始浏览分类
                </Link>
                <Link className="rounded-full border border-black/10 px-5 py-2 font-medium" href="/search">
                  进入搜索页
                </Link>
              </div>
            </div>
            <div className="flex flex-col gap-4 rounded-[28px] bg-white/75 p-5">
              <SearchForm />
              {user ? (
                <div className="flex flex-col gap-4 rounded-[24px] border border-black/10 bg-[var(--surface)] p-4">
                  <p className="text-sm leading-6 text-black/65">
                    当前登录用户：<span className="font-semibold">{user.nickname}</span>（{user.roles.join(", ")}）
                  </p>
                  <div className="flex flex-wrap gap-3">
                    <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/account">
                      账户页
                    </Link>
                    <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/cart">
                      购物车
                    </Link>
                    <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/account/addresses">
                      收货地址
                    </Link>
                    <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/admin">
                      后台页
                    </Link>
                    <LogoutButton className="rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white" />
                  </div>
                </div>
              ) : (
                <div className="flex flex-wrap gap-3 rounded-[24px] border border-black/10 bg-[var(--surface)] p-4">
                  <Link className="rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white" href="/login">
                    去登录
                  </Link>
                  <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/register">
                    去注册
                  </Link>
                </div>
              )}
            </div>
          </div>
        </section>

        <CategoryDirectory categories={categories.slice(0, 6)} />

        <StorefrontProductList
          emptyDescription="当前还没有可展示的首页商品。"
          products={products.items}
          title="首页推荐商品"
        />
      </div>
    </main>
  );
}
