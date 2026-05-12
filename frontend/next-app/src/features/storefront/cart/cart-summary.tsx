import Link from "next/link";

import type { CartSummary as CartSummaryModel } from "@/lib/cart/types";

type CartSummaryProps = {
  summary: CartSummaryModel;
};

export function CartSummary({ summary }: CartSummaryProps) {
  return (
    <aside className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
      <div className="space-y-2">
        <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
          Cart Summary
        </span>
        <h2 className="text-2xl font-semibold tracking-tight">本次勾选汇总</h2>
        <p className="text-sm leading-7 text-black/65">购物车页只做勾选和数量管理，真正的异常识别与地址确认放到下一步汇总页。</p>
      </div>

      <dl className="mt-6 grid gap-4 rounded-[24px] bg-[var(--surface)] p-4">
        <Metric label="勾选条目" value={`${summary.selectedItemCount} 件`} />
        <Metric label="勾选数量" value={`${summary.selectedQuantity} 件`} />
        <Metric label="勾选金额" value={formatPrice(summary.selectedAmount)} />
      </dl>

      <div className="mt-6 flex flex-col gap-3">
        <Link
          aria-disabled={summary.selectedItemCount === 0}
          className={`rounded-full px-5 py-3 text-center text-sm font-semibold text-white ${
            summary.selectedItemCount === 0 ? "cursor-not-allowed bg-black/30" : "bg-[var(--accent)]"
          }`}
          href={summary.selectedItemCount === 0 ? "/cart" : "/checkout-summary"}
        >
          去结算前汇总
        </Link>
        <Link className="rounded-full border border-black/10 px-5 py-3 text-center text-sm font-medium" href="/account/addresses">
          管理收货地址
        </Link>
      </div>
    </aside>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-black/6 pb-3 last:border-b-0 last:pb-0">
      <dt className="text-sm text-black/50">{label}</dt>
      <dd className="text-lg font-semibold">{value}</dd>
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}
