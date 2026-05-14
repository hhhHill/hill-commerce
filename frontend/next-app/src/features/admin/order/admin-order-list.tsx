"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { triggerAutoComplete } from "@/lib/admin/client";
import type { AdminOrderListResult, AdminOrderStatus } from "@/lib/admin/types";

type AdminOrderListProps = {
  result: AdminOrderListResult;
  currentStatus?: AdminOrderStatus;
};

const STATUS_OPTIONS: Array<{ label: string; value?: AdminOrderStatus }> = [
  { label: "全部订单" },
  { label: "待支付", value: "PENDING_PAYMENT" },
  { label: "已支付", value: "PAID" },
  { label: "已发货", value: "SHIPPED" },
  { label: "已完成", value: "COMPLETED" },
  { label: "已取消", value: "CANCELLED" },
  { label: "已关闭", value: "CLOSED" }
];

export function AdminOrderList({ result, currentStatus }: AdminOrderListProps) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  return (
    <div className="space-y-6">
      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex flex-wrap gap-3">
            {STATUS_OPTIONS.map((option) => {
              const active = option.value === currentStatus || (!option.value && !currentStatus);
              return (
                <Link
                  key={option.label}
                  className={
                    active
                      ? "rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white"
                      : "rounded-full border border-black/10 px-4 py-2 text-sm font-medium text-black/70"
                  }
                  href={`/admin/orders${option.value ? `?status=${option.value}` : ""}`}
                >
                  {option.label}
                </Link>
              );
            })}
          </div>
          <button
            className="rounded-full border border-black/10 px-5 py-3 text-sm font-semibold"
            disabled={isPending}
            type="button"
            onClick={() => {
              setMessage("");
              startTransition(async () => {
                try {
                  const result = await triggerAutoComplete();
                  setMessage(`自动完成执行完毕，本次完成 ${result.completedCount} 笔订单`);
                  router.refresh();
                } catch (error) {
                  setMessage(error instanceof Error ? error.message : "触发自动完成失败");
                }
              });
            }}
          >
            {isPending ? "执行中..." : "触发自动完成"}
          </button>
        </div>
        {message ? <p className="mt-4 text-sm text-black/65">{message}</p> : null}
      </section>

      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-semibold">订单列表</h2>
            <p className="mt-2 text-sm text-black/65">支持按状态筛选，并从已支付订单进入发货表单。</p>
          </div>
          <span className="rounded-full bg-[#f4e0cc] px-3 py-1 text-sm font-medium text-[var(--accent-strong)]">
            共 {result.total} 笔
          </span>
        </div>
        <div className="mt-5 space-y-4">
          {result.items.map((order) => (
            <article key={order.orderId} className="rounded-[24px] border border-black/10 bg-[#fffaf5] p-5 shadow-[0_10px_26px_rgba(29,20,13,0.04)]">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="space-y-3">
                  <div className="flex flex-wrap items-center gap-3">
                    <span className="rounded-full bg-white px-3 py-1 text-sm font-medium">{renderStatus(order.orderStatus)}</span>
                    <span className="text-sm text-black/50">{formatDateTime(order.createdAt)}</span>
                  </div>
                  <h3 className="text-xl font-semibold">{order.orderNo}</h3>
                  <p className="text-sm text-black/65">用户 ID：{order.userId}</p>
                  <p className="text-sm text-black/65">{renderSummary(order.summaryProductName, order.summaryItemCount)}</p>
                </div>
                <div className="flex flex-col items-start gap-3 lg:items-end">
                  <p className="text-2xl font-semibold text-[var(--accent-strong)]">{formatPrice(order.payableAmount)}</p>
                  {order.orderStatus === "PAID" ? (
                    <Link className="rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white" href={`/admin/orders/${order.orderId}/ship`}>
                      发货
                    </Link>
                  ) : (
                    <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href={`/orders/${order.orderId}`}>
                      查看详情
                    </Link>
                  )}
                </div>
              </div>
            </article>
          ))}
          {result.items.length === 0 ? (
            <p className="rounded-[24px] border border-dashed border-black/10 px-5 py-10 text-center text-sm text-black/55">
              当前筛选下没有订单。
            </p>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function renderStatus(status: AdminOrderStatus) {
  switch (status) {
    case "PENDING_PAYMENT":
      return "待支付";
    case "PAID":
      return "已支付";
    case "SHIPPED":
      return "已发货";
    case "COMPLETED":
      return "已完成";
    case "CANCELLED":
      return "已取消";
    case "CLOSED":
      return "已关闭";
    default:
      return status;
  }
}

function renderSummary(summaryProductName: string | null, summaryItemCount: number) {
  if (!summaryProductName) {
    return "暂无商品摘要";
  }
  if (summaryItemCount <= 1) {
    return summaryProductName;
  }
  return `${summaryProductName} 等 ${summaryItemCount} 件商品`;
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
