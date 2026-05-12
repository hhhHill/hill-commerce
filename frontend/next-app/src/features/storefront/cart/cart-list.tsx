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
    <div className="flex flex-col gap-4">
      {items.map((item) => (
        <article
          key={item.id}
          className="grid gap-5 rounded-[28px] border border-black/10 bg-white/85 p-5 shadow-[0_18px_50px_rgba(74,42,18,0.06)] md:grid-cols-[140px_1fr]"
        >
          <Link
            className="overflow-hidden rounded-[24px] border border-black/10 bg-[linear-gradient(160deg,#f4e7d2_0%,#e7d1ba_100%)]"
            href={`/products/${item.productId}`}
          >
            {item.productCoverImageUrl ? (
              <img alt={item.productName} className="aspect-square h-full w-full object-cover" src={item.productCoverImageUrl} />
            ) : (
              <div className="flex aspect-square items-center justify-center text-sm font-medium text-black/40">暂无图片</div>
            )}
          </Link>

          <div className="flex flex-col gap-4">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="space-y-2">
                <Link className="text-2xl font-semibold tracking-tight" href={`/products/${item.productId}`}>
                  {item.productName}
                </Link>
                <p className="text-sm text-black/55">{item.skuSpecText}</p>
                <p className="text-sm text-black/45">{item.skuCode}</p>
              </div>

              <div className="rounded-[22px] bg-[var(--surface)] px-4 py-3 text-right">
                <p className="text-sm text-black/50">小计</p>
                <p className="text-2xl font-semibold text-[var(--accent-strong)]">{formatPrice(item.subtotalAmount)}</p>
              </div>
            </div>

            <div className="grid gap-4 rounded-[22px] border border-black/10 bg-[var(--surface)] px-4 py-4 md:grid-cols-[1fr_auto] md:items-center">
              <div className="flex flex-wrap gap-6 text-sm text-black/65">
                <span>单价：{formatPrice(item.unitPrice)}</span>
                <span>数量：{item.quantity}</span>
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
