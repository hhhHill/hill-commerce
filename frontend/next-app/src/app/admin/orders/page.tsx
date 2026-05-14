import { AdminOrderList } from "@/features/admin/order/admin-order-list";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { requireRole } from "@/lib/auth/server";
import { getAdminOrders } from "@/lib/admin/server";
import type { AdminOrderStatus } from "@/lib/admin/types";

type AdminOrdersPageProps = {
  searchParams: Promise<{
    status?: string;
    page?: string;
  }>;
};

const ALLOWED_STATUSES = new Set<AdminOrderStatus>(["PENDING_PAYMENT", "PAID", "SHIPPED", "COMPLETED", "CANCELLED", "CLOSED"]);

export default async function AdminOrdersPage({ searchParams }: AdminOrdersPageProps) {
  const user = await requireRole(["ADMIN", "SALES"], "/admin/orders");
  const { status, page } = await searchParams;
  const currentStatus = parseStatus(status);
  const currentPage = parsePositiveInteger(page) ?? 1;
  const result = await getAdminOrders(currentStatus, currentPage, 10);

  return (
    <AdminShell
      description="后台订单管理支持全状态筛选、发货入口和手动触发自动完成，用于覆盖 PAID 之后的履约链路。"
      title="订单管理"
      user={user}
    >
      <AdminOrderList currentStatus={currentStatus} result={result} />
    </AdminShell>
  );
}

function parseStatus(value?: string): AdminOrderStatus | undefined {
  if (!value) {
    return undefined;
  }
  return ALLOWED_STATUSES.has(value as AdminOrderStatus) ? (value as AdminOrderStatus) : undefined;
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
