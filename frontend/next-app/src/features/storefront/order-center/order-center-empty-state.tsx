import { OrderEmptyState } from "@/features/storefront/order/order-empty-state";
import type { OrderListStatus } from "@/lib/order/types";

type OrderCenterEmptyStateProps = {
  status?: OrderListStatus;
  orderNo?: string;
};

export function OrderCenterEmptyState({ status, orderNo }: OrderCenterEmptyStateProps) {
  if (orderNo?.trim()) {
    return (
      <OrderEmptyState
        description={`没有找到订单号前缀与“${orderNo.trim()}”匹配的订单记录。`}
        primaryHref="/orders"
        primaryLabel="返回全部订单"
        secondaryHref="/"
        secondaryLabel="继续浏览商品"
        title="没有匹配的订单"
      />
    );
  }

  if (status) {
    return (
      <OrderEmptyState
        description={`当前筛选条件下暂时没有“${renderStatus(status)}”订单。`}
        primaryHref="/orders"
        primaryLabel="查看全部订单"
        secondaryHref="/"
        secondaryLabel="继续浏览商品"
        title="筛选结果为空"
      />
    );
  }

  return (
    <OrderEmptyState
      description="你还没有生成任何订单记录。可以先从商品浏览、购物车或结算链路继续操作。"
      primaryHref="/categories"
      primaryLabel="去逛商品"
      secondaryHref="/cart"
      secondaryLabel="查看购物车"
      title="还没有订单"
    />
  );
}

function renderStatus(status: OrderListStatus) {
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
