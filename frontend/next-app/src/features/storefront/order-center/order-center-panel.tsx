import { OrderCenterEmptyState } from "@/features/storefront/order-center/order-center-empty-state";
import { OrderCenterList } from "@/features/storefront/order-center/order-center-list";
import { OrderCenterPagination } from "@/features/storefront/order-center/order-center-pagination";
import { OrderCenterToolbar } from "@/features/storefront/order-center/order-center-toolbar";
import type { OrderListResult, OrderListStatus } from "@/lib/order/types";

type OrderCenterPanelProps = {
  result: OrderListResult;
  status?: OrderListStatus;
  orderNo?: string;
};

export function OrderCenterPanel({ result, status, orderNo }: OrderCenterPanelProps) {
  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-2">
          <span className="chip-badge w-fit">我的订单</span>
          <h1 className="text-3xl font-semibold tracking-tight">订单查询中心</h1>
          <p className="max-w-3xl text-sm leading-7 text-[var(--text-secondary)]">这里负责回访已创建订单、按状态查看和按订单号快速定位，不重新定义订单详情和支付规则。</p>
        </div>
        <div className="chip-badge">共 {result.total} 笔订单</div>
      </div>

      <OrderCenterToolbar currentOrderNo={orderNo} currentStatus={status} />

      {result.items.length === 0 ? (
        <OrderCenterEmptyState orderNo={orderNo} status={status} />
      ) : (
        <>
          <OrderCenterList items={result.items} />
          <OrderCenterPagination orderNo={orderNo} page={result.page} status={status} totalPages={result.totalPages} />
        </>
      )}
    </section>
  );
}
