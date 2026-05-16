import { AnalyticsShell } from "@/features/admin/analytics/analytics-shell";
import { ClientProductAnalytics } from "@/features/admin/analytics/products/client-product-analytics";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { requireRole } from "@/lib/auth/server";

export default async function AnalyticsProductsPage() {
  const user = await requireRole(["ADMIN", "SALES"], "/admin/analytics/products");

  return (
    <AdminShell description="按今日、本周、本月查看商品销量排行和品类占比。" title="商品分析" user={user}>
      <AnalyticsShell active="products">
        <ClientProductAnalytics />
      </AnalyticsShell>
    </AdminShell>
  );
}
