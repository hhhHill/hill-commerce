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
    <main className="page-shell pb-32">
      <div className="page-stack">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-3">
            <HomeShortcut />
            <Link className="btn-secondary px-4 py-2" href="/checkout-summary">
              返回结算前汇总
            </Link>
          </div>
          <SearchForm />
        </div>

        <section className="space-y-3">
          <span className="chip-badge">下单确认</span>
          <h1 className="text-3xl font-semibold tracking-tight">最终结算确认</h1>
          <p className="max-w-3xl text-sm leading-7 text-[var(--text-secondary)]">这一步会消费当前已勾选购物车项与默认地址。点击提交订单后，服务端会再次校验库存、SKU 状态、商品状态和地址有效性。</p>
        </section>

        <CheckoutPanel checkout={checkout} />
      </div>
    </main>
  );
}
