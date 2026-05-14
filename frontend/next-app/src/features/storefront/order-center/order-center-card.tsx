import Link from "next/link";

import type { OrderListItem } from "@/lib/order/types";

type OrderCenterCardProps = {
  order: OrderListItem;
};

export function OrderCenterCard({ order }: OrderCenterCardProps) {
  return (
    <article className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_50px_rgba(74,42,18,0.06)]">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-3">
            <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
              {renderStatus(order.orderStatus)}
            </span>
            <span className="text-sm text-black/45">{formatDateTime(order.createdAt)}</span>
          </div>
          <Link className="text-2xl font-semibold tracking-tight" href={`/orders/${order.orderId}`}>
            {order.orderNo}
          </Link>
          <p className="text-sm leading-7 text-black/65">{renderSummary(order.summaryProductName, order.summaryItemCount)}</p>
        </div>

        <div className="flex flex-col items-end gap-3">
          <div className="text-right">
            <p className="text-sm text-black/45">订单金额</p>
            <p className="text-2xl font-semibold text-[var(--accent-strong)]">{formatPrice(order.payableAmount)}</p>
          </div>
          <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href={`/orders/${order.orderId}`}>
            查看详情
          </Link>
        </div>
      </div>
    </article>
  );
}

function renderSummary(summaryProductName: string | null, summaryItemCount: number) {
  if (!summaryProductName) {
    return "当前订单暂时没有可展示的商品摘要。";
  }
  if (summaryItemCount <= 1) {
    return summaryProductName;
  }
  return `${summaryProductName} 等 ${summaryItemCount} 件商品`;
}

function renderStatus(status: string) {
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

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("zh-CN", {
    hour12: false
  });
}
