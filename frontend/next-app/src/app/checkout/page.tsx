import Link from "next/link";

import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { CheckoutPanel } from "@/features/storefront/order/checkout-panel";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { getServerOrderCheckout } from "@/lib/order/server";

export default async function CheckoutPage() {
  await requireUser("/checkout");
  const checkout = await getServerOrderCheckout();

  return (
    <main className="min-h-screen px-6 py-10">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-3">
            <HomeShortcut />
            <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/checkout-summary">
              返回结算前汇总
            </Link>
          </div>
          <SearchForm className="w-full max-w-md" />
        </div>

        <section className="space-y-3">
          <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
            Checkout
          </span>
          <h1 className="text-4xl font-semibold tracking-tight">最终结算确认</h1>
          <p className="max-w-3xl text-sm leading-7 text-black/65">这一步会消费当前已勾选购物车项与默认地址。点击提交订单后，服务端会再次校验库存、SKU 状态、商品状态和地址有效性。</p>
        </section>

        <CheckoutPanel checkout={checkout} />
      </div>
    </main>
  );
}
