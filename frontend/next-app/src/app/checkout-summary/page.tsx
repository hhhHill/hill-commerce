import Link from "next/link";

import { CheckoutSummaryPanel } from "@/features/storefront/cart/checkout-summary-panel";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { getServerCheckoutSummary } from "@/lib/cart/server";

export default async function CheckoutSummaryPage() {
  await requireUser("/checkout-summary");
  const summary = await getServerCheckoutSummary();

  return (
    <main className="min-h-screen px-6 py-10">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/cart">
            返回购物车
          </Link>
          <SearchForm className="w-full max-w-md" />
        </div>

        <section className="space-y-3">
          <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
            Pre Checkout
          </span>
          <h1 className="text-4xl font-semibold tracking-tight">结算前汇总与异常识别</h1>
          <p className="max-w-3xl text-sm leading-7 text-black/65">这一页只做“是否具备进入下单阶段的前置条件”判断，不真正创建订单。失效商品、SKU 异常、库存不足和地址缺失都会在这里被拦住。</p>
        </section>

        <CheckoutSummaryPanel summary={summary} />
      </div>
    </main>
  );
}
