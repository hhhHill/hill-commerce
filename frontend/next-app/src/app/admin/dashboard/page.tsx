import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AdminDashboard } from "@/features/admin/dashboard/admin-dashboard";
import { requireRole } from "@/lib/auth/server";
import { getServerDashboardSummary } from "@/lib/admin/server";

export default async function AdminDashboardPage() {
  const user = await requireRole(["ADMIN"], "/admin/dashboard");
  const summary = await getServerDashboardSummary();

  return (
    <AdminShell
      description="实时查看订单状态、支付后销售额，以及 Sales 发货量排行。"
      title="仪表盘"
      user={user}
    >
      <AdminDashboard summary={summary} />
    </AdminShell>
  );
}
