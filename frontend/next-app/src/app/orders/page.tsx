import Link from "next/link";

import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { OrderCenterPanel } from "@/features/storefront/order-center/order-center-panel";
import { requireUser } from "@/lib/auth/server";
import { getServerOrders } from "@/lib/order/server";
import type { OrderListQuery, OrderListStatus } from "@/lib/order/types";

type OrdersPageProps = {
  searchParams: Promise<{
    page?: string;
    size?: string;
    status?: string;
    orderNo?: string;
  }>;
};

const ALLOWED_STATUSES = new Set<OrderListStatus>(["PENDING_PAYMENT", "PAID", "SHIPPED", "COMPLETED", "CANCELLED", "CLOSED"]);

export default async function OrdersPage({ searchParams }: OrdersPageProps) {
  await requireUser("/orders");

  const { page, size, status, orderNo } = await searchParams;
  const query: OrderListQuery = {
    page: parsePositiveInteger(page),
    size: parsePositiveInteger(size),
    status: parseStatus(status),
    orderNo: orderNo?.trim() ? orderNo.trim() : undefined
  };
  const result = await getServerOrders(query);

  return (
    <main className="page-shell">
      <div className="page-stack">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-3">
            <HomeShortcut />
            <Link className="btn-secondary px-4 py-2" href="/account">
              返回账户页
            </Link>
          </div>
          <Link className="btn-secondary px-4 py-2" href="/cart">
            前往购物车
          </Link>
        </div>

        <OrderCenterPanel orderNo={query.orderNo} result={result} status={query.status} />
      </div>
    </main>
  );
}

function parsePositiveInteger(value?: string) {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) {
    return undefined;
  }
  return parsed;
}

function parseStatus(value?: string): OrderListStatus | undefined {
  if (!value) {
    return undefined;
  }
  return ALLOWED_STATUSES.has(value as OrderListStatus) ? (value as OrderListStatus) : undefined;
}
