"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";

import { updateCartItem } from "@/lib/cart/client";
import type { CartItem, CartSummary as CartSummaryModel } from "@/lib/cart/types";

type CartBottomBarProps = {
  summary: CartSummaryModel;
  items: CartItem[];
};

export function CartBottomBar({ summary, items }: CartBottomBarProps) {
  const router = useRouter();
  const canCheckout = summary.selectedItemCount > 0;
  const allSelected = items.length > 0 && items.every((item) => item.selected);

  const handleToggleAll = async () => {
    const newSelected = !allSelected;
    try {
      await Promise.all(
        items.map((item) =>
          updateCartItem(item.id, { quantity: item.quantity, selected: newSelected })
        )
      );
      router.refresh();
    } catch {
      // silently fail
    }
  };

  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-[#f0f0f0] bg-white">
      <div className="mx-auto flex max-w-[800px] items-center gap-4 px-4 py-3">
        {/* select all */}
        <button
          onClick={handleToggleAll}
          className="flex items-center gap-2 text-sm cursor-pointer hover:opacity-80"
        >
          <span
            className={`inline-flex h-5 w-5 items-center justify-center rounded-full ${
              allSelected ? "bg-[#ff5000]" : "border-2 border-[#e0e0e0] bg-white"
            }`}
          >
            {allSelected && (
              <svg
                className="h-3 w-3 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  d="M5 13l4 4L19 7"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2.5}
                />
              </svg>
            )}
          </span>
          全选
        </button>

        <div className="min-w-0 flex-1 text-right">
          <p className="text-xs text-[var(--text-hint)]">
            已选{" "}
            <span className="font-medium text-[var(--text-primary)]">
              {summary.selectedItemCount}
            </span>{" "}
            件
            {summary.selectedQuantity > summary.selectedItemCount
              ? ` / ${summary.selectedQuantity} 个`
              : ""}
            ，不含运费
          </p>
          <p
            className="text-lg font-bold text-[var(--price)]"
            style={{ fontFamily: "var(--font-price)" }}
          >
            {formatPrice(summary.selectedAmount)}
          </p>
        </div>

        <Link
          className={`shrink-0 rounded-full px-8 py-2.5 text-sm font-semibold text-white ${
            canCheckout ? "bg-[#ff5000]" : "pointer-events-none bg-[#ccc]"
          }`}
          href={canCheckout ? "/checkout" : "/cart"}
        >
          结算{canCheckout ? `(${summary.selectedItemCount})` : ""}
        </Link>
      </div>
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}
