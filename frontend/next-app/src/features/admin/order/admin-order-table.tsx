"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { triggerAutoComplete } from "@/lib/admin/client";
import type { AdminOrderListResult, AdminOrderStatus } from "@/lib/admin/types";

type AdminOrderTableProps = {
  result: AdminOrderListResult;
  currentStatus?: AdminOrderStatus;
};

export function AdminOrderTable({ result, currentStatus }: AdminOrderTableProps) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  return (
    <div className="flex flex-col">
      {/* top bar */}
      <div className="flex items-center justify-between border-b border-[#f0f0f0] px-4 py-2">
        <p className="text-sm text-[var(--text-secondary)]">
          共 <span className="font-semibold text-[var(--text-primary)]">{result.total}</span> 笔订单
          {result.totalPages > 1 ? (
            <span className="ml-2">
              第 {result.page}/{result.totalPages} 页
            </span>
          ) : null}
        </p>
        <div className="flex items-center gap-3">
          {message ? (
            <p className="text-sm text-[var(--text-secondary)]">{message}</p>
          ) : null}
          <button
            className="rounded-[2px] border border-[#f0f0f0] px-3 py-1.5 text-xs text-[var(--text-secondary)] hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
            disabled={isPending}
            type="button"
            onClick={() => {
              setMessage("");
              startTransition(async () => {
                try {
                  const res = await triggerAutoComplete();
                  setMessage(`自动完成 ${res.completedCount} 笔`);
                  router.refresh();
                } catch (error) {
                  setMessage(error instanceof Error ? error.message : "失败");
                }
              });
            }}
          >
            {isPending ? "执行中..." : "触发自动完成"}
          </button>
        </div>
      </div>

      {/* table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#f0f0f0] text-left text-xs text-[var(--text-hint)]">
              <th className="px-4 py-2.5 font-medium">订单号</th>
              <th className="px-4 py-2.5 font-medium">用户ID</th>
              <th className="px-4 py-2.5 font-medium">商品摘要</th>
              <th className="px-4 py-2.5 font-medium text-right">金额</th>
              <th className="px-4 py-2.5 font-medium">状态</th>
              <th className="px-4 py-2.5 font-medium">下单时间</th>
              <th className="px-4 py-2.5 font-medium text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            {result.items.map((order) => (
              <tr
                key={order.orderId}
                className="border-b border-[#f5f5f5] hover:bg-[#fafafa] transition-colors"
              >
                <td className="px-4 py-2.5">
                  <Link
                    className="font-medium text-[var(--text-primary)] hover:text-[var(--brand-primary)]"
                    href={`/orders/${order.orderId}`}
                  >
                    {order.orderNo}
                  </Link>
                </td>
                <td className="px-4 py-2.5 text-[var(--text-secondary)]">
                  {order.userId}
                </td>
                <td className="px-4 py-2.5 max-w-[200px] truncate text-[var(--text-secondary)]">
                  {order.summaryProductName ?? "—"}
                  {order.summaryItemCount > 1
                    ? ` 等${order.summaryItemCount}件`
                    : ""}
                </td>
                <td className="px-4 py-2.5 text-right font-semibold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
                  {formatPrice(order.payableAmount)}
                </td>
                <td className="px-4 py-2.5">
                  <span
                    className={`inline-flex rounded-[2px] px-1.5 py-0.5 text-xs font-medium ${
                      order.orderStatus === "PAID"
                        ? "bg-orange-50 text-orange-700"
                        : order.orderStatus === "SHIPPED"
                          ? "bg-blue-50 text-blue-700"
                          : order.orderStatus === "COMPLETED"
                            ? "bg-emerald-50 text-emerald-700"
                            : order.orderStatus === "PENDING_PAYMENT"
                              ? "bg-amber-50 text-amber-700"
                              : "bg-gray-100 text-gray-600"
                    }`}
                  >
                    {renderStatus(order.orderStatus)}
                  </span>
                </td>
                <td className="px-4 py-2.5 whitespace-nowrap text-[var(--text-hint)]">
                  {formatDateTime(order.createdAt)}
                </td>
                <td className="px-4 py-2.5 text-right">
                  {order.orderStatus === "PAID" ? (
                    <Link
                      className="rounded-[2px] bg-[var(--brand-primary)] px-3 py-1 text-xs font-medium text-white hover:bg-[var(--brand-deep)]"
                      href={`/admin/orders/${order.orderId}/ship`}
                    >
                      发货
                    </Link>
                  ) : (
                    <Link
                      className="text-xs text-[var(--text-secondary)] hover:text-[var(--brand-primary)]"
                      href={`/orders/${order.orderId}`}
                    >
                      查看
                    </Link>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* empty */}
      {result.items.length === 0 ? (
        <p className="px-4 py-10 text-center text-sm text-[var(--text-hint)]">
          当前筛选下没有订单
        </p>
      ) : null}

      {/* pagination */}
      {result.totalPages > 1 ? (
        <div className="flex items-center justify-between border-t border-[#f0f0f0] px-4 py-2">
          <p className="text-xs text-[var(--text-hint)]">
            {result.page}/{result.totalPages} 页 · 共 {result.total} 笔
          </p>
          <div className="flex gap-2">
            {result.page > 1 ? (
              <Link
                className="rounded-[2px] border border-[#f0f0f0] px-3 py-1 text-xs text-[var(--text-secondary)] hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={`/admin/orders?${buildPageQuery(currentStatus, result.page - 1)}`}
              >
                上一页
              </Link>
            ) : null}
            {result.page < result.totalPages ? (
              <Link
                className="rounded-[2px] border border-[#f0f0f0] px-3 py-1 text-xs text-[var(--text-secondary)] hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={`/admin/orders?${buildPageQuery(currentStatus, result.page + 1)}`}
              >
                下一页
              </Link>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}

function buildPageQuery(status: AdminOrderStatus | undefined, page: number): string {
  const sp = new URLSearchParams();
  if (status) sp.set("status", status);
  sp.set("page", String(page));
  return sp.toString();
}

function renderStatus(status: AdminOrderStatus) {
  switch (status) {
    case "PENDING_PAYMENT": return "待支付";
    case "PAID": return "未发货";
    case "SHIPPED": return "已发货";
    case "COMPLETED": return "已完成";
    case "CANCELLED": return "已取消";
    case "CLOSED": return "已关闭";
    default: return status;
  }
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
