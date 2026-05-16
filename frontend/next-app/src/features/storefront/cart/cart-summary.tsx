import Link from "next/link";

import type { CartSummary as CartSummaryModel } from "@/lib/cart/types";

type CartSummaryProps = {
  summary: CartSummaryModel;
};

export function CartSummary({ summary }: CartSummaryProps) {
  return (
    <>
      <aside className="surface-card hidden rounded-lg p-5 lg:block">
        <div className="space-y-2">
          <span className="chip-badge">购物车汇总</span>
          <h2 className="text-2xl font-semibold tracking-tight">本次勾选汇总</h2>
          <p className="text-sm leading-7 text-[var(--text-secondary)]">购物车页只做勾选和数量管理，真正的异常识别与地址确认放到下一步汇总页。</p>
        </div>

        <dl className="surface-subtle mt-4 grid gap-4 p-4">
          <Metric label="勾选条目" value={`${summary.selectedItemCount} 件`} />
          <Metric label="勾选数量" value={`${summary.selectedQuantity} 件`} />
          <Metric label="勾选金额" value={formatPrice(summary.selectedAmount)} />
        </dl>

        <div className="mt-4 flex flex-col gap-3">
          <Link
            aria-disabled={summary.selectedItemCount === 0}
            className={`btn-primary w-full ${summary.selectedItemCount === 0 ? "btn-disabled" : ""}`}
            href={summary.selectedItemCount === 0 ? "/cart" : "/checkout-summary"}
          >
            去结算前汇总
          </Link>
          <Link className="btn-secondary w-full" href="/account/addresses">
            管理收货地址
          </Link>
        </div>
      </aside>

      <aside className="fixed inset-x-0 bottom-16 z-40 border-t border-[var(--border-normal)] bg-white px-3 py-3 shadow-[0_-6px_20px_rgba(0,0,0,0.08)] lg:hidden">
        <div className="mx-auto flex max-w-[1200px] items-center gap-3">
          <div className="min-w-0 flex-1">
            <p className="text-xs text-[var(--text-secondary)]">已选 {summary.selectedItemCount} 件 / {summary.selectedQuantity} 个</p>
            <p className="mt-1 text-2xl font-bold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
              {formatPrice(summary.selectedAmount)}
            </p>
          </div>
          <Link
            aria-disabled={summary.selectedItemCount === 0}
            className={`btn-primary shrink-0 px-5 ${summary.selectedItemCount === 0 ? "btn-disabled" : ""}`}
            href={summary.selectedItemCount === 0 ? "/cart" : "/checkout-summary"}
          >
            去结算
          </Link>
        </div>
      </aside>
    </>
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
