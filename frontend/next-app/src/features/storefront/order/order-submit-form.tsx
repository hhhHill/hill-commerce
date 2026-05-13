"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { createOrder } from "@/lib/order/client";

type OrderSubmitFormProps = {
  canSubmit: boolean;
};

export function OrderSubmitForm({ canSubmit }: OrderSubmitFormProps) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  return (
    <div className="space-y-3">
      <button
        className={`w-full rounded-full px-5 py-3 text-center text-sm font-semibold text-white ${
          canSubmit ? "bg-[var(--accent)]" : "bg-black/30"
        }`}
        disabled={!canSubmit || isPending}
        type="button"
        onClick={() => {
          setMessage("");
          startTransition(async () => {
            try {
              const order = await createOrder();
              router.push(`/orders/${order.orderId}/result`);
              router.refresh();
            } catch (error) {
              setMessage(error instanceof Error ? error.message : "提交订单失败");
            }
          });
        }}
      >
        {isPending ? "提交中..." : "提交订单"}
      </button>
      {message ? <p className="text-sm text-red-700">{message}</p> : null}
    </div>
  );
}
