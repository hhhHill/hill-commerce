"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { cancelOrder } from "@/lib/order/client";
import type { OrderDetail } from "@/lib/order/types";

type OrderResultPanelProps = {
  order: OrderDetail;
};

export function OrderResultPanel({ order }: OrderResultPanelProps) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();
  const canCancel = order.orderStatus === "PENDING_PAYMENT";

  return (
    <section className="grid gap-6 lg:grid-cols-[1fr_0.95fr]">
      <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
        <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
          Order Created
        </span>
        <h1 className="mt-3 text-4xl font-semibold tracking-tight">订单已创建，等待支付</h1>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-black/65">当前 `order-checkout` 阶段已经完成：订单、订单项、地址快照和库存扣减都已落库。下一阶段 `payment` 会承接支付成功与超时关闭。</p>

        <dl className="mt-6 grid gap-4 rounded-[24px] bg-[var(--surface)] p-4">
          <Metric label="订单号" value={order.orderNo} />
          <Metric label="订单状态" value={renderStatus(order.orderStatus)} />
          <Metric label="应付金额" value={formatPrice(order.payableAmount)} />
          <Metric label="支付截止" value={formatDateTime(order.paymentDeadlineAt)} />
        </dl>

        <div className="mt-6 flex flex-wrap gap-3">
          <Link className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white" href={`/orders/${order.id}`}>
            查看订单详情
          </Link>
          <Link className="rounded-full border border-black/10 px-5 py-3 text-sm font-medium" href="/cart">
            返回购物车
          </Link>
        </div>
      </article>

      <aside className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
        <h2 className="text-2xl font-semibold tracking-tight">后续动作</h2>
        <p className="mt-2 text-sm leading-7 text-black/65">本阶段先把订单创建稳定下来。支付能力尚未接入，但这里已经保留了结果页和未支付取消入口，便于下一阶段继续串联。</p>

        <div className="mt-6 rounded-[22px] bg-[var(--surface)] px-4 py-4 text-sm text-black/70">
          <p className="font-semibold text-black">收货地址</p>
          <p className="mt-2 leading-7">
            {order.address.receiverName} · {order.address.receiverPhone}
          </p>
          <p className="leading-7">
            {order.address.province}
            {order.address.city}
            {order.address.district}
            {order.address.detailAddress}
          </p>
        </div>

        <div className="mt-6 flex flex-col gap-3">
          <span className="rounded-full bg-black/5 px-5 py-3 text-center text-sm font-medium text-black/55">去支付入口将在 payment feature 接入</span>
          {canCancel ? (
            <button
              className="rounded-full border border-red-200 px-5 py-3 text-sm font-semibold text-red-700"
              disabled={isPending}
              type="button"
              onClick={() => {
                setMessage("");
                startTransition(async () => {
                  try {
                    await cancelOrder(order.id);
                    router.refresh();
                  } catch (error) {
                    setMessage(error instanceof Error ? error.message : "取消订单失败");
                  }
                });
              }}
            >
              {isPending ? "取消中..." : "取消未支付订单"}
            </button>
          ) : (
            <div className="rounded-[22px] bg-emerald-50 px-4 py-4 text-sm font-medium text-emerald-700">当前订单状态为 {renderStatus(order.orderStatus)}，不再显示未支付取消入口。</div>
          )}
          {message ? <p className="text-sm text-red-700">{message}</p> : null}
        </div>
      </aside>
    </section>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-black/6 pb-3 last:border-b-0 last:pb-0">
      <dt className="text-sm text-black/50">{label}</dt>
      <dd className="text-right text-lg font-semibold">{value}</dd>
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "未设置";
  }

  return new Date(value).toLocaleString("zh-CN", {
    hour12: false
  });
}

function renderStatus(status: string) {
  switch (status) {
    case "PENDING_PAYMENT":
      return "待支付";
    case "CANCELLED":
      return "已取消";
    case "PAID":
      return "已支付";
    default:
      return status;
  }
}
