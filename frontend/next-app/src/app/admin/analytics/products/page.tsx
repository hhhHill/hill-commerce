import { AnalyticsShell } from "@/features/admin/analytics/analytics-shell";
import { ClientProductAnalytics } from "@/features/admin/analytics/products/client-product-analytics";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { requireRole } from "@/lib/auth/server";
import { getServerViewLogs } from "@/lib/admin/server";

export default async function AnalyticsProductsPage(props: {
  searchParams: Promise<{ tab?: string; productId?: string; categoryId?: string }>;
}) {
  const user = await requireRole(["ADMIN", "MERCHANT"], "/admin/analytics/products");
  const searchParams = await props.searchParams;
  const isViewLogsTab = searchParams.tab === "view-logs";

  const viewLogs = isViewLogsTab
    ? await getServerViewLogs({
        productId: searchParams.productId ? Number(searchParams.productId) : undefined,
        categoryId: searchParams.categoryId ? Number(searchParams.categoryId) : undefined,
      })
    : undefined;

  return (
    <AdminShell description="按今日、本周、本月查看商品销量排行和品类占比。" title="商品分析" user={user}>
      <AnalyticsShell active="products" user={user}>
        <ClientProductAnalytics
          viewLogs={viewLogs}
          viewLogFilters={{
            productId: searchParams.productId,
            categoryId: searchParams.categoryId,
          }}
        />
      </AnalyticsShell>
    </AdminShell>
  );
}
