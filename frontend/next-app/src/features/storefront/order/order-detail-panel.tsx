"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { cancelOrder, confirmReceipt } from "@/lib/order/client";
import type { OrderDetail } from "@/lib/order/types";

type OrderDetailPanelProps = {
  order: OrderDetail;
};

export function OrderDetailPanel({ order }: OrderDetailPanelProps) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();
  const canCancel = order.orderStatus === "PENDING_PAYMENT";
  const canPay = order.orderStatus === "PENDING_PAYMENT";
  const canConfirmReceipt = order.orderStatus === "SHIPPED";

  return (
    <section className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
      <div className="flex flex-col gap-4">
        <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
                Order Detail
              </span>
              <h1 className="mt-3 text-4xl font-semibold tracking-tight">{order.orderNo}</h1>
            </div>
            <span className="rounded-full border border-black/10 px-4 py-2 text-sm font-semibold">{renderStatus(order.orderStatus)}</span>
          </div>

          <dl className="mt-6 grid gap-4 rounded-[24px] bg-[var(--surface)] p-4">
            <Metric label="订单金额" value={formatPrice(order.totalAmount)} />
            <Metric label="应付金额" value={formatPrice(order.payableAmount)} />
            <Metric label="支付截止" value={formatDateTime(order.paymentDeadlineAt)} />
          </dl>
        </article>

        <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
          <h2 className="text-2xl font-semibold tracking-tight">商品快照</h2>
          <div className="mt-5 flex flex-col gap-4">
            {order.items.map((item) => (
              <div key={item.id} className="grid gap-4 rounded-[24px] border border-black/10 bg-[var(--surface)] p-4 md:grid-cols-[120px_1fr]">
                <Link
                  className="overflow-hidden rounded-[20px] border border-black/10 bg-[linear-gradient(160deg,#f4e7d2_0%,#e7d1ba_100%)]"
                  href={`/products/${item.productId}`}
                >
                  {item.productImageUrl ? (
                    <img alt={item.productName} className="aspect-square h-full w-full object-cover" src={item.productImageUrl} />
                  ) : (
                    <div className="flex aspect-square items-center justify-center text-sm font-medium text-black/40">暂无图片</div>
                  )}
                </Link>
                <div className="flex flex-col gap-3">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <Link className="text-xl font-semibold" href={`/products/${item.productId}`}>
                        {item.productName}
                      </Link>
                      <p className="mt-2 text-sm text-black/55">{item.skuSpecText}</p>
                      <p className="mt-1 text-sm text-black/45">{item.skuCode}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm text-black/50">小计</p>
                      <p className="text-2xl font-semibold text-[var(--accent-strong)]">{formatPrice(item.subtotalAmount)}</p>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-6 text-sm text-black/65">
                    <span>单价：{formatPrice(item.unitPrice)}</span>
                    <span>数量：{item.quantity}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </article>

        {(order.orderStatus === "SHIPPED" || order.orderStatus === "COMPLETED") && order.shipment ? (
          <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
            <h2 className="text-2xl font-semibold tracking-tight">物流信息</h2>
            <div className="mt-5 grid gap-4 rounded-[24px] bg-[var(--surface)] p-4">
              <Metric label="快递公司" value={order.shipment.carrierName} />
              <div className="flex items-center justify-between gap-4 border-b border-black/6 pb-3">
                <dt className="text-sm text-black/50">运单号</dt>
                <dd className="flex items-center gap-3 text-right">
                  <span className="text-lg font-semibold">{order.shipment.trackingNo}</span>
                  <button
                    className="rounded-full border border-black/10 px-3 py-1 text-xs font-semibold"
                    type="button"
                    onClick={async () => {
                      try {
                        await navigator.clipboard.writeText(order.shipment?.trackingNo ?? "");
                        setMessage("运单号已复制");
                      } catch {
                        setMessage("复制失败，请手动复制运单号");
                      }
                    }}
                  >
                    复制
                  </button>
                </dd>
              </div>
              <Metric label="发货时间" value={formatDateTime(order.shipment.shippedAt)} />
            </div>
          </article>
        ) : null}

        <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
          <h2 className="text-2xl font-semibold tracking-tight">状态历史</h2>
          <div className="mt-5 flex flex-col gap-3">
            {order.statusHistory.map((history) => (
              <div key={history.id} className="rounded-[22px] border border-black/10 bg-[var(--surface)] px-4 py-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <p className="text-sm font-semibold">
                    {history.fromStatus ? `${renderStatus(history.fromStatus)} → ` : ""}
                    {renderStatus(history.toStatus)}
                  </p>
                  <p className="text-sm text-black/50">{formatDateTime(history.createdAt)}</p>
                </div>
                {history.changeReason ? <p className="mt-2 text-sm text-black/65">{history.changeReason}</p> : null}
              </div>
            ))}
          </div>
        </article>
      </div>

      <aside className="flex flex-col gap-4">
        <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
          <h2 className="text-2xl font-semibold tracking-tight">收货地址快照</h2>
          <p className="mt-4 text-lg font-semibold">{order.address.receiverName}</p>
          <p className="mt-1 text-sm text-black/60">{order.address.receiverPhone}</p>
          <p className="mt-4 text-sm leading-7 text-black/65">
            {order.address.province}
            {order.address.city}
            {order.address.district}
            {order.address.detailAddress}
            {order.address.postalCode ? ` · ${order.address.postalCode}` : ""}
          </p>
        </article>

        <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
          <h2 className="text-2xl font-semibold tracking-tight">订单动作</h2>
          <div className="mt-5 flex flex-col gap-3">
            {canPay ? (
              <Link className="rounded-full bg-[var(--accent)] px-5 py-3 text-center text-sm font-semibold text-white" href={`/pay/${order.id}`}>
                去支付
              </Link>
            ) : (
              <div className="rounded-[22px] bg-black/5 px-4 py-4 text-sm font-medium text-black/60">{renderPaymentHint(order.orderStatus)}</div>
            )}
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
              <div className="rounded-[22px] bg-emerald-50 px-4 py-4 text-sm font-medium text-emerald-700">当前订单状态为 {renderStatus(order.orderStatus)}，不再支持未支付取消。</div>
            )}
            {canConfirmReceipt ? (
              <button
                className="rounded-full bg-[var(--accent-strong)] px-5 py-3 text-sm font-semibold text-white"
                disabled={isPending}
                type="button"
                onClick={() => {
                  if (!window.confirm("确认已收到商品？确认后订单将变更为已完成。")) {
                    return;
                  }
                  setMessage("");
                  startTransition(async () => {
                    try {
                      await confirmReceipt(order.id);
                      router.refresh();
                    } catch (error) {
                      setMessage(error instanceof Error ? error.message : "确认收货失败");
                    }
                  });
                }}
              >
                {isPending ? "提交中..." : "确认收货"}
              </button>
            ) : null}
            <Link className="rounded-full border border-black/10 px-5 py-3 text-center text-sm font-medium" href="/cart">
              返回购物车
            </Link>
            <Link className="rounded-full border border-black/10 px-5 py-3 text-center text-sm font-medium" href="/orders">
              返回我的订单
            </Link>
            {message ? <p className="text-sm text-red-700">{message}</p> : null}
          </div>
        </article>
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
    case "CLOSED":
      return "已关闭";
    case "PAID":
      return "已支付";
    case "SHIPPED":
      return "已发货";
    case "COMPLETED":
      return "已完成";
    default:
      return status;
  }
}

function renderPaymentHint(status: string) {
  switch (status) {
    case "PAID":
      return "当前订单已支付，无需再次发起支付。";
    case "CANCELLED":
      return "当前订单已取消，支付入口已关闭。";
    case "CLOSED":
      return "当前订单已关闭，支付入口已关闭。";
    case "SHIPPED":
      return "当前订单已发货，请在收到商品后确认收货。";
    case "COMPLETED":
      return "当前订单已完成，无需再次支付。";
    default:
      return "当前订单不处于可支付状态。";
  }
}
