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
    getServerStorefrontProducts({ pageSize: 12 })
  ]);

  return (
    <main className="page-shell pb-24">
      <div className="page-stack">
        <section className="flex flex-col gap-3">
          <SearchForm />
          {user ? (
            <div className="flex flex-wrap items-center gap-3 text-sm text-[var(--text-secondary)]">
              <span>
                当前账号：<span className="font-semibold text-[var(--text-primary)]">{user.nickname}</span>
              </span>
              <Link className="btn-ghost px-0 py-0 text-sm" href="/account">
                我的账户
              </Link>
              <Link className="btn-ghost px-0 py-0 text-sm" href="/orders">
                我的订单
              </Link>
              <Link className="btn-ghost px-0 py-0 text-sm" href="/account/addresses">
                收货地址
              </Link>
              <Link className="btn-ghost px-0 py-0 text-sm" href="/admin">
                后台页
              </Link>
              <LogoutButton className="btn-secondary px-3 py-1.5 text-sm" />
            </div>
          ) : (
            <div className="flex flex-wrap items-center gap-3 text-sm text-[var(--text-secondary)]">
              <Link className="btn-ghost px-0 py-0 text-sm" href="/login">
                登录
              </Link>
              <Link className="btn-ghost px-0 py-0 text-sm" href="/register">
                注册
              </Link>
              <Link className="btn-ghost px-0 py-0 text-sm" href="/search">
                全站搜索
              </Link>
            </div>
          )}
        </section>

        <CategoryDirectory categories={categories.slice(0, 10)} />

        <StorefrontProductList
          emptyDescription="当前还没有可展示的首页商品。"
          products={products.items}
          title="猜你喜欢"
        />
      </div>
    </main>
  );
}
