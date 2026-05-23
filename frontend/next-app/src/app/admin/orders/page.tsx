import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AdminOrderTable } from "@/features/admin/order/admin-order-table";
import { requireRole } from "@/lib/auth/server";
import { getAdminOrders } from "@/lib/admin/server";
import type { AdminOrderStatus } from "@/lib/admin/types";

type AdminOrdersPageProps = {
  searchParams: Promise<{
    status?: string;
    page?: string;
  }>;
};

const ALLOWED_STATUSES = new Set<AdminOrderStatus>([
  "PENDING_PAYMENT", "PAID", "SHIPPED", "COMPLETED", "CANCELLED", "CLOSED"
]);

export default async function AdminOrdersPage({ searchParams }: AdminOrdersPageProps) {
  const user = await requireRole(["ADMIN", "MERCHANT"], "/admin/orders");
  const { status, page } = await searchParams;
  const currentStatus = parseStatus(status);
  const currentPage = parsePositiveInteger(page) ?? 1;
  const result = await getAdminOrders(currentStatus, currentPage, 20);

  return (
    <AdminShell
      description=""
      title="订单管理"
      user={user}
    >
      <AdminOrderTable currentStatus={currentStatus} result={result} />
    </AdminShell>
  );
}

function parseStatus(value?: string): AdminOrderStatus | undefined {
  if (!value) return undefined;
  return ALLOWED_STATUSES.has(value as AdminOrderStatus) ? (value as AdminOrderStatus) : undefined;
}

function parsePositiveInteger(value?: string) {
  if (!value) return undefined;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 1 ? parsed : undefined;
}
