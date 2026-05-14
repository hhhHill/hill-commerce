"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { PaymentActions } from "@/features/storefront/payment/payment-actions";
import { PaymentAttemptCard } from "@/features/storefront/payment/payment-attempt-card";
import { PaymentEmptyState } from "@/features/storefront/payment/payment-empty-state";
import { PaymentStatePanel } from "@/features/storefront/payment/payment-state-panel";
import { PaymentSummaryCard } from "@/features/storefront/payment/payment-summary-card";
import { createPaymentAttempt, failPayment, succeedPayment } from "@/lib/payment/client";
import type { PaymentOrder } from "@/lib/payment/types";

type PaymentPanelProps = {
  payment: PaymentOrder;
};

export function PaymentPanel({ payment }: PaymentPanelProps) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  const canPay = payment.orderStatus === "PENDING_PAYMENT";
  const canActOnAttempt = canPay && payment.currentAttempt?.paymentStatus === "INITIATED";
  const canCreateAttempt = canPay && (!payment.currentAttempt || payment.currentAttempt.paymentStatus === "FAILED");

  return (
    <section className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
      <div className="flex flex-col gap-4">
        <PaymentSummaryCard payment={payment} />
        {payment.currentAttempt ? <PaymentAttemptCard attempt={payment.currentAttempt} /> : <PaymentEmptyState orderId={payment.orderId} />}
      </div>

      <aside className="flex flex-col gap-4">
        {canPay ? (
          <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
            <h2 className="text-2xl font-semibold tracking-tight">支付动作</h2>
            <p className="mt-2 text-sm leading-7 text-black/65">MVP 先用模拟支付承接订单状态推进。成功会把订单推进到已支付，失败则保留待支付状态并允许重新创建尝试。</p>

            <div className="mt-6 flex flex-col gap-3">
              <PaymentActions
                busy={isPending}
                canActOnAttempt={canActOnAttempt}
                canCreateAttempt={canCreateAttempt}
                onCreateAttempt={() => {
                  setMessage("");
                  startTransition(async () => {
                    try {
                      await createPaymentAttempt(payment.orderId);
                      router.refresh();
                    } catch (error) {
                      setMessage(error instanceof Error ? error.message : "创建支付尝试失败");
                    }
                  });
                }}
                onFail={() => {
                  if (!payment.currentAttempt) {
                    return;
                  }

                  setMessage("");
                  startTransition(async () => {
                    try {
                      await failPayment(payment.currentAttempt!.paymentId);
                      router.refresh();
                    } catch (error) {
                      setMessage(error instanceof Error ? error.message : "模拟支付失败请求未完成");
                    }
                  });
                }}
                onSucceed={() => {
                  if (!payment.currentAttempt) {
                    return;
                  }

                  setMessage("");
                  startTransition(async () => {
                    try {
                      await succeedPayment(payment.currentAttempt!.paymentId);
                      router.push(`/orders/${payment.orderId}/result`);
                      router.refresh();
                    } catch (error) {
                      setMessage(error instanceof Error ? error.message : "模拟支付成功请求未完成");
                    }
                  });
                }}
              />

              {message ? <p className="text-sm text-red-700">{message}</p> : null}
            </div>

            <div className="mt-6 flex flex-col gap-3">
              <Link className="rounded-full border border-black/10 px-5 py-3 text-center text-sm font-medium" href={`/orders/${payment.orderId}/result`}>
                返回订单结果页
              </Link>
              <Link className="rounded-full border border-black/10 px-5 py-3 text-center text-sm font-medium" href={`/orders/${payment.orderId}`}>
                查看订单详情
              </Link>
            </div>
          </article>
        ) : (
          <PaymentStatePanel orderId={payment.orderId} orderStatus={payment.orderStatus} />
        )}
      </aside>
    </section>
  );
}
