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
        <h1 className="text-3xl font-semibold tracking-tight">我的订单</h1>
        <span className="text-sm text-[var(--text-hint)]">共 {result.total} 笔订单</span>
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
