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
    <div className="flex flex-col">
      {items.map((item) => (
        <div
          key={item.id}
          className="flex items-start gap-3 border-b border-[#f5f5f5] py-3"
        >
          {/* select circle */}
          <div
            className={`mt-4 h-5 w-5 shrink-0 rounded-full border-2 ${
              item.canCheckout
                ? "border-[#ff5000] bg-[#ff5000]"
                : "border-[#e0e0e0] bg-white"
            }`}
          />

          {/* product image */}
          <Link
            className="h-[72px] w-[72px] shrink-0 overflow-hidden rounded bg-[#f5f5f5]"
            href={`/products/${item.productId}`}
          >
            {item.productCoverImageUrl ? (
              <img
                alt={item.productName}
                className="h-full w-full object-cover"
                src={item.productCoverImageUrl}
              />
            ) : (
              <div className="flex h-full items-center justify-center text-xs text-[var(--text-hint)]">
                暂无图片
              </div>
            )}
          </Link>

          {/* info + actions */}
          <div className="flex min-w-0 flex-1 flex-col gap-1">
            <Link
              className="line-clamp-2 text-sm leading-5"
              href={`/products/${item.productId}`}
            >
              {item.productName}
            </Link>
            <p className="text-xs text-[var(--text-hint)]">{item.skuSpecText}</p>

            {/* anomaly hint — inline red text, no banner */}
            {item.canCheckout ? null : (
              <p className="text-xs font-medium text-red-500">
                {item.anomalyMessage ?? "当前不可结算"}
              </p>
            )}

            <div className="mt-1 flex items-center justify-between">
              <span
                className="text-sm font-semibold text-[var(--price)]"
                style={{ fontFamily: "var(--font-price)" }}
              >
                {formatPrice(item.subtotalAmount)}
              </span>
              <CartItemActions item={item} />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}
