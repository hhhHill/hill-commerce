import Link from "next/link";

import { CartItemActions } from "@/features/storefront/cart/cart-item-actions";
import { CartEmptyState } from "@/features/storefront/cart/cart-empty-state";
import type { CartItem } from "@/lib/cart/types";

type CartListProps = {
  items: CartItem[];
};

export function CartList({ items }: CartListProps) {
  if (items.length === 0) {
    return <CartEmptyState />;
  }

  return (
    <div className="flex flex-col gap-3">
      {items.map((item) => (
        <article
          key={item.id}
          className="surface-card grid gap-3 rounded-lg p-3 md:grid-cols-[112px_1fr]"
        >
          <Link
            className="overflow-hidden rounded-lg border border-[var(--border-normal)] bg-[var(--border-light)]"
            href={`/products/${item.productId}`}
          >
            {item.productCoverImageUrl ? (
              <img alt={item.productName} className="aspect-square h-full w-full object-cover" src={item.productCoverImageUrl} />
            ) : (
              <div className="flex aspect-square items-center justify-center text-sm font-medium text-[var(--text-hint)]">暂无图片</div>
            )}
          </Link>

          <div className="flex flex-col gap-3">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="space-y-2">
                <Link className="text-lg font-semibold tracking-tight" href={`/products/${item.productId}`}>
                  {item.productName}
                </Link>
                <p className="text-sm text-[var(--text-secondary)]">{item.skuSpecText}</p>
                <p className="text-xs text-[var(--text-hint)]">{item.skuCode}</p>
              </div>

              <div className="text-right">
                <p className="text-xs text-[var(--text-secondary)]">小计</p>
                <p className="text-2xl font-bold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
                  {formatPrice(item.subtotalAmount)}
                </p>
              </div>
            </div>

            <div className="surface-subtle grid gap-3 px-3 py-3 md:grid-cols-[1fr_auto] md:items-center">
              <div className="space-y-3">
                <div className="flex flex-wrap gap-4 text-sm text-[var(--text-secondary)]">
                  <span>单价：{formatPrice(item.unitPrice)}</span>
                  <span>数量：{item.quantity}</span>
                </div>
                {item.canCheckout ? null : (
                  <p className="rounded-lg bg-red-50 px-3 py-2 text-sm font-medium text-red-700">
                    {item.anomalyMessage ?? "当前条目暂不可结算"}
                  </p>
                )}
              </div>
              <CartItemActions item={item} />
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}
