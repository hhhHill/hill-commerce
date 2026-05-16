import Link from "next/link";

import { CheckoutAddressCard } from "@/features/storefront/order/checkout-address-card";
import { CheckoutItemList } from "@/features/storefront/order/checkout-item-list";
import { OrderEmptyState } from "@/features/storefront/order/order-empty-state";
import { OrderSubmitForm } from "@/features/storefront/order/order-submit-form";
import type { OrderCheckout } from "@/lib/order/types";

type CheckoutPanelProps = {
  checkout: OrderCheckout;
};

export function CheckoutPanel({ checkout }: CheckoutPanelProps) {
  if (checkout.items.length === 0) {
    return (
      <OrderEmptyState
        description="当前没有可进入下单确认页的勾选商品。先回到购物车或结算前汇总，完成勾选与预校验。"
        primaryHref="/cart"
        primaryLabel="返回购物车"
        secondaryHref="/checkout-summary"
        secondaryLabel="查看结算前汇总"
        title="没有待确认的订单条目"
      />
    );
  }

  return (
    <section className="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
      <div className="flex flex-col gap-4">
        <CheckoutAddressCard address={checkout.defaultAddress} />
        <CheckoutItemList items={checkout.items} />
      </div>

      <aside className="surface-card rounded-lg p-5">
        <span className="chip-badge">Final Checkout</span>
        <h2 className="mt-3 text-2xl font-semibold tracking-tight">真正创建订单前的最终确认</h2>
        <p className="mt-2 text-sm leading-7 text-[var(--text-secondary)]">这里不再承担重新勾选和地址编辑，只消费当前勾选条目与默认地址，并在提交时由服务端再次做最终校验。</p>

        <dl className="surface-subtle mt-6 grid gap-4 p-4">
          <Metric label="勾选条目" value={`${checkout.summary.selectedItemCount} 件`} />
          <Metric label="勾选数量" value={`${checkout.summary.selectedQuantity} 件`} />
          <Metric label="合计金额" value={formatPrice(checkout.summary.totalAmount)} />
          <Metric label="有效条目" value={`${checkout.summary.validSelectedItemCount} 件`} />
          <Metric label="有效金额" value={formatPrice(checkout.summary.validTotalAmount)} />
        </dl>

        {checkout.summary.blockingReasons.length > 0 ? (
          <div className="mt-6 rounded-[22px] border border-red-200 bg-red-50 px-4 py-4">
            <p className="text-sm font-semibold text-red-800">当前仍存在阻断原因</p>
            <ul className="mt-3 flex flex-col gap-2 text-sm text-red-700">
              {checkout.summary.blockingReasons.map((reason) => (
                <li key={reason}>{renderBlockingReason(reason)}</li>
              ))}
            </ul>
          </div>
        ) : null}

        <div
          className={`mt-6 rounded-[22px] px-4 py-4 text-sm font-medium ${
            checkout.summary.canSubmit ? "bg-emerald-50 text-emerald-800" : "bg-amber-50 text-amber-800"
          }`}
        >
          {checkout.summary.canSubmit ? "当前输入条件完整，可以发起真实订单创建。" : "当前仍有异常或缺失信息，系统不会创建订单。"}
        </div>

        <div className="mt-6 flex flex-col gap-3">
          <Link className="btn-secondary w-full px-5 py-3" href="/checkout-summary">
            返回结算前汇总
          </Link>
          <OrderSubmitForm canSubmit={checkout.summary.canSubmit} />
        </div>
      </aside>
    </section>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-black/6 pb-3 last:border-b-0 last:pb-0">
      <dt className="text-sm text-[var(--text-secondary)]">{label}</dt>
      <dd className="text-lg font-semibold">{value}</dd>
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}

function renderBlockingReason(reason: string) {
  switch (reason) {
    case "MISSING_DEFAULT_ADDRESS":
      return "缺少默认地址";
    case "NO_SELECTED_ITEMS":
      return "当前没有勾选商品";
    case "PRODUCT_OFF_SHELF":
      return "存在已下架商品";
    case "SKU_INVALID":
      return "存在已失效的 SKU";
    case "SKU_DISABLED":
      return "存在已禁用的 SKU";
    case "INSUFFICIENT_STOCK":
      return "存在库存不足的商品";
    default:
      return reason;
  }
}
