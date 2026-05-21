"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { PaymentActions } from "@/features/storefront/payment/payment-actions";
import { PaymentAttemptCard } from "@/features/storefront/payment/payment-attempt-card";
import { PaymentEmptyState } from "@/features/storefront/payment/payment-empty-state";
import { PaymentStatePanel } from "@/features/storefront/payment/payment-state-panel";
import {
  createPaymentAttempt,
  failPayment,
  succeedPayment,
} from "@/lib/payment/client";
import type { PaymentOrder } from "@/lib/payment/types";

type PaymentPanelProps = {
  payment: PaymentOrder;
};

export function PaymentPanel({ payment }: PaymentPanelProps) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  const canPay = payment.orderStatus === "PENDING_PAYMENT";
  const canActOnAttempt =
    canPay && payment.currentAttempt?.paymentStatus === "INITIATED";
  const canCreateAttempt =
    canPay &&
    (!payment.currentAttempt ||
      payment.currentAttempt.paymentStatus === "FAILED");

  return (
    <div className="flex flex-col">
      {/* order summary — flat rows */}
      <div className="border-b border-[#f0f0f0] py-4">
        <div className="flex justify-between py-1 text-sm">
          <span className="text-[var(--text-secondary)]">订单号</span>
          <span>{payment.orderNo}</span>
        </div>
        <div className="flex justify-between py-1 text-sm">
          <span className="text-[var(--text-secondary)]">应付金额</span>
          <span
            className="font-semibold text-[var(--price)]"
            style={{ fontFamily: "var(--font-price)" }}
          >
            {formatPrice(payment.payableAmount)}
          </span>
        </div>
        <div className="flex justify-between py-1 text-sm">
          <span className="text-[var(--text-secondary)]">订单状态</span>
          <span>{renderOrderStatus(payment.orderStatus)}</span>
        </div>
      </div>

      {/* payment attempt */}
      {payment.currentAttempt ? (
        <PaymentAttemptCard attempt={payment.currentAttempt} />
      ) : (
        <PaymentEmptyState orderId={payment.orderId} />
      )}

      {/* actions */}
      {canPay ? (
        <div className="mt-4 space-y-3">
          <p className="text-sm text-[var(--text-hint)]">
            支付方式：模拟支付
          </p>

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
                  setMessage(
                    error instanceof Error
                      ? error.message
                      : "创建支付尝试失败"
                  );
                }
              });
            }}
            onFail={() => {
              if (!payment.currentAttempt) return;
              setMessage("");
              startTransition(async () => {
                try {
                  await failPayment(payment.currentAttempt!.paymentId);
                  router.refresh();
                } catch (error) {
                  setMessage(
                    error instanceof Error
                      ? error.message
                      : "支付失败请求未完成"
                  );
                }
              });
            }}
            onSucceed={() => {
              if (!payment.currentAttempt) return;
              setMessage("");
              startTransition(async () => {
                try {
                  await succeedPayment(payment.currentAttempt!.paymentId);
                  router.push(`/orders/${payment.orderId}/result`);
                  router.refresh();
                } catch (error) {
                  setMessage(
                    error instanceof Error
                      ? error.message
                      : "支付成功请求未完成"
                  );
                }
              });
            }}
          />

          {message ? (
            <p className="text-sm text-red-500">{message}</p>
          ) : null}
        </div>
      ) : (
        <PaymentStatePanel
          orderId={payment.orderId}
          orderStatus={payment.orderStatus}
        />
      )}

      {/* nav links */}
      <div className="mt-6 flex gap-3 border-t border-[#f0f0f0] pt-4">
        <Link
          className="text-sm text-[var(--text-secondary)]"
          href={`/orders/${payment.orderId}`}
        >
          查看订单详情
        </Link>
        <Link
          className="text-sm text-[var(--text-secondary)]"
          href="/cart"
        >
          返回购物车
        </Link>
      </div>
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}

function renderOrderStatus(status: string) {
  switch (status) {
    case "PENDING_PAYMENT":
      return "待支付";
    case "PAID":
      return "已支付";
    case "CANCELLED":
      return "已取消";
    case "CLOSED":
      return "已关闭";
    default:
      return status;
  }
}
