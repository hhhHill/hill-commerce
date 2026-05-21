import Link from "next/link";

import type { OrderCheckoutItem } from "@/lib/order/types";

type CheckoutItemListProps = {
  items: OrderCheckoutItem[];
};

export function CheckoutItemList({ items }: CheckoutItemListProps) {
  return (
    <div className="mx-4 flex flex-col">
      {/* shop label */}
      <p className="py-3 text-xs text-[var(--text-hint)]">商品清单</p>

      {items.map((item) => (
        <div
          key={item.id}
          className="flex gap-3 border-b border-[#f5f5f5] py-3"
        >
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

          {/* info */}
          <div className="flex min-w-0 flex-1 flex-col gap-1">
            <Link
              className="line-clamp-2 text-sm leading-5"
              href={`/products/${item.productId}`}
            >
              {item.productName}
            </Link>
            <p className="text-xs text-[var(--text-hint)]">{item.skuSpecText}</p>

            {!item.canSubmit ? (
              <p className="text-xs text-red-500">
                {item.anomalyMessage ?? "暂不可提交"}
              </p>
            ) : null}

            <div className="mt-1 flex items-center justify-between">
              <span
                className="text-sm font-semibold text-[var(--price)]"
                style={{ fontFamily: "var(--font-price)" }}
              >
                {formatPrice(item.subtotalAmount)}
              </span>
              <span className="text-xs text-[var(--text-hint)]">
                ×{item.quantity}
              </span>
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
