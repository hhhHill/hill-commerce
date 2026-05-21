"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { createOrder } from "@/lib/order/client";

type CheckoutBottomBarProps = {
  canSubmit: boolean;
};

export function CheckoutBottomBar({ canSubmit }: CheckoutBottomBarProps) {
  const router = useRouter();
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  function handleSubmit() {
    setError("");
    startTransition(async () => {
      try {
        const result = await createOrder();
        router.push(`/orders/${result.orderId}/result`);
      } catch (e) {
        setError(e instanceof Error ? e.message : "提交订单失败，请重试");
      }
    });
  }

  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-[#f0f0f0] bg-white">
      <div className="mx-auto flex max-w-[800px] items-center gap-4 px-4 py-3">
        {error ? (
          <p className="flex-1 text-sm text-red-500">{error}</p>
        ) : (
          <p className="flex-1 text-sm text-[var(--text-secondary)]">
            合计见上方明细
          </p>
        )}
        <button
          className={`shrink-0 rounded-full px-10 py-2.5 text-sm font-semibold text-white ${
            canSubmit && !isPending
              ? "bg-[#ff5000]"
              : "cursor-not-allowed bg-[#ccc]"
          }`}
          disabled={!canSubmit || isPending}
          onClick={handleSubmit}
        >
          {isPending ? "提交中..." : "提交订单"}
        </button>
      </div>
    </div>
  );
}
