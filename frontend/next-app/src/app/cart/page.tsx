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
    <main className="min-h-screen px-6 py-10">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-3">
            <HomeShortcut />
            <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/categories">
              继续逛商品
            </Link>
          </div>
          <SearchForm className="w-full max-w-md" />
        </div>

        <section className="space-y-3">
          <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
            Cart
          </span>
          <h1 className="text-4xl font-semibold tracking-tight">购物车与勾选准备</h1>
          <p className="max-w-3xl text-sm leading-7 text-black/65">这里负责数量调整、条目删除和结算前勾选。真正的地址校验和失效项识别，会在下一步汇总页统一完成。</p>
        </section>

        <section className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <CartList items={cart.items} />
          <CartSummary summary={cart.summary} />
        </section>
      </div>
    </main>
  );
}
