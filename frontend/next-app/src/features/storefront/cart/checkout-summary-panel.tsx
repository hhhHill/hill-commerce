import Link from "next/link";

import { CartEmptyState } from "@/features/storefront/cart/cart-empty-state";
import type { CheckoutSummary } from "@/lib/cart/types";

type CheckoutSummaryPanelProps = {
  summary: CheckoutSummary;
};

export function CheckoutSummaryPanel({ summary }: CheckoutSummaryPanelProps) {
  if (summary.items.length === 0) {
    return (
      <CartEmptyState
        actionHref="/cart"
        actionLabel="返回购物车"
        description="当前没有勾选任何购物车条目，先回到购物车完成勾选。"
        title="没有待汇总的商品"
      />
    );
  }

  return (
    <section className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
      <div className="flex flex-col gap-4">
        {summary.defaultAddress ? (
          <article className="surface-card rounded-lg p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm uppercase tracking-[0.18em] text-[var(--text-secondary)]">Default Address</p>
                <h2 className="mt-2 text-2xl font-semibold">{summary.defaultAddress.receiverName}</h2>
                <p className="mt-1 text-sm text-[var(--text-secondary)]">{summary.defaultAddress.receiverPhone}</p>
              </div>
              <Link className="btn-secondary px-4 py-2" href="/account/addresses">
                管理地址
              </Link>
            </div>
            <p className="mt-4 text-sm leading-7 text-[var(--text-secondary)]">
              {summary.defaultAddress.province}
              {summary.defaultAddress.city}
              {summary.defaultAddress.district}
              {summary.defaultAddress.detailAddress}
              {summary.defaultAddress.postalCode ? ` · ${summary.defaultAddress.postalCode}` : ""}
            </p>
          </article>
        ) : (
          <article className="rounded-[28px] border border-dashed border-red-200 bg-red-50/80 p-5 text-red-800">
            <h2 className="text-xl font-semibold">还没有默认地址</h2>
            <p className="mt-2 text-sm leading-7">当前无法进入真正下单流程。先补充地址，系统会自动维护默认地址。</p>
            <Link className="btn-primary mt-4 inline-flex px-4 py-2" href="/account/addresses">
              去新增地址
            </Link>
          </article>
        )}

        <div className="flex flex-col gap-4">
          {summary.items.map((item) => (
            <article key={item.id} className="surface-card rounded-lg p-4">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-2">
                  <h3 className="text-xl font-semibold">{item.productName}</h3>
                  <p className="text-sm text-[var(--text-secondary)]">{item.skuSpecText}</p>
                  <p className="text-sm text-[var(--text-hint)]">{item.skuCode}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm text-[var(--text-secondary)]">小计</p>
                  <p className="text-2xl font-bold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
                    {formatPrice(item.subtotalAmount)}
                  </p>
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-4 text-sm text-[var(--text-secondary)]">
                <span>单价：{formatPrice(item.unitPrice)}</span>
                <span>数量：{item.quantity}</span>
              </div>

              {item.canCheckout ? (
                <p className="mt-4 rounded-full bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-700">当前条目可参与下单</p>
              ) : (
                <p className="mt-4 rounded-[20px] bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
                  {item.anomalyMessage ?? "当前条目暂不可结算"}
                </p>
              )}
            </article>
          ))}
        </div>
      </div>

      <aside className="surface-card rounded-lg p-5">
        <span className="chip-badge">Summary Gate</span>
        <h2 className="mt-3 text-2xl font-semibold tracking-tight">进入下单前的最后检查</h2>

        <dl className="surface-subtle mt-6 grid gap-4 p-4">
          <Metric label="勾选条目" value={`${summary.summary.selectedItemCount} 件`} />
          <Metric label="勾选数量" value={`${summary.summary.selectedQuantity} 件`} />
          <Metric label="勾选金额" value={formatPrice(summary.summary.selectedAmount)} />
          <Metric label="有效条目" value={`${summary.summary.validSelectedItemCount} 件`} />
          <Metric label="有效金额" value={formatPrice(summary.summary.validSelectedAmount)} />
        </dl>

        <div className="mt-6 space-y-3">
          {summary.summary.blockingReasons.length > 0 ? (
            <div className="rounded-[22px] border border-red-200 bg-red-50 px-4 py-4">
              <p className="text-sm font-semibold text-red-800">当前阻断原因</p>
              <ul className="mt-3 flex flex-col gap-2 text-sm text-red-700">
                {summary.summary.blockingReasons.map((reason) => (
                  <li key={reason}>{renderBlockingReason(reason)}</li>
                ))}
              </ul>
            </div>
          ) : null}

          <div
            className={`rounded-[22px] px-4 py-4 text-sm font-medium ${
              summary.summary.canProceed ? "bg-emerald-50 text-emerald-800" : "bg-amber-50 text-amber-800"
            }`}
          >
            {summary.summary.canProceed ? "当前输入条件完整，已具备进入真正下单阶段的前置条件。" : "当前仍有异常或缺失信息，不能继续进入下单阶段。"}
          </div>
        </div>

        <div className="mt-6 flex flex-col gap-3">
          <Link className="btn-secondary w-full px-5 py-3" href="/cart">
            返回购物车调整
          </Link>
          <Link
            aria-disabled={!summary.summary.canProceed}
            className={`btn-primary w-full px-5 py-3 ${summary.summary.canProceed ? "" : "btn-disabled"}`}
            href={summary.summary.canProceed ? "/checkout" : "/checkout-summary"}
          >
            {summary.summary.canProceed ? "继续进入最终下单确认" : "当前不可继续"}
          </Link>
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
