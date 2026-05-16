import Link from "next/link";

import { CheckoutSummaryPanel } from "@/features/storefront/cart/checkout-summary-panel";
import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { getServerCheckoutSummary } from "@/lib/cart/server";

export default async function CheckoutSummaryPage() {
  await requireUser("/checkout-summary");
  const summary = await getServerCheckoutSummary();

  return (
    <main className="page-shell pb-32">
      <div className="page-stack">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-3">
            <HomeShortcut />
            <Link className="btn-secondary px-4 py-2" href="/cart">
              返回购物车
            </Link>
          </div>
          <SearchForm />
        </div>

        <section className="space-y-3">
          <span className="chip-badge">结算前汇总</span>
          <h1 className="text-3xl font-semibold tracking-tight">结算前汇总与异常识别</h1>
          <p className="max-w-3xl text-sm leading-7 text-[var(--text-secondary)]">这一页只做“是否具备进入下单阶段的前置条件”判断，不真正创建订单。失效商品、SKU 异常、库存不足和地址缺失都会在这里被拦住。</p>
        </section>

        <CheckoutSummaryPanel summary={summary} />
      </div>
    </main>
  );
}
