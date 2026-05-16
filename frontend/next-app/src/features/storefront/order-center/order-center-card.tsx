import Link from "next/link";

import type { OrderListItem } from "@/lib/order/types";

type OrderCenterCardProps = {
  order: OrderListItem;
};

export function OrderCenterCard({ order }: OrderCenterCardProps) {
  return (
    <article className="surface-card rounded-lg p-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-3">
            <span className="chip-badge">
              {renderStatus(order.orderStatus)}
            </span>
            <span className="text-sm text-[var(--text-hint)]">{formatDateTime(order.createdAt)}</span>
          </div>
          <Link className="text-xl font-semibold tracking-tight" href={`/orders/${order.orderId}`}>
            {order.orderNo}
          </Link>
          <p className="text-sm leading-7 text-[var(--text-secondary)]">{renderSummary(order.summaryProductName, order.summaryItemCount)}</p>
        </div>

        <div className="flex flex-col items-end gap-3">
          <div className="text-right">
            <p className="text-sm text-[var(--text-hint)]">订单金额</p>
            <p className="text-2xl font-bold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
              {formatPrice(order.payableAmount)}
            </p>
          </div>
          <Link className="btn-secondary px-4 py-2" href={`/orders/${order.orderId}`}>
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
