import Link from "next/link";

import { CartList } from "@/features/storefront/cart/cart-list";
import { CartSummary } from "@/features/storefront/cart/cart-summary";
import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { getServerCart } from "@/lib/cart/server";

export default async function CartPage() {
  await requireUser("/cart");
  const cart = await getServerCart();

  return (
    <main className="page-shell pb-36 lg:pb-5">
      <div className="page-stack">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-3">
            <HomeShortcut />
            <Link className="btn-secondary px-4 py-2" href="/categories">
              继续逛商品
            </Link>
          </div>
          <SearchForm />
        </div>

        <section className="space-y-3">
          <span className="chip-badge">购物车</span>
          <h1 className="text-3xl font-semibold tracking-tight">购物车与勾选准备</h1>
          <p className="max-w-3xl text-sm leading-7 text-[var(--text-secondary)]">这里负责数量调整、条目删除和结算前勾选。真正的地址校验和失效项识别，会在下一步汇总页统一完成。</p>
        </section>

        <section className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
          <CartList items={cart.items} />
          <CartSummary summary={cart.summary} />
        </section>
      </div>
    </main>
  );
}
