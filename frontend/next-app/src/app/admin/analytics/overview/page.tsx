import { AnalyticsShell } from "@/features/admin/analytics/analytics-shell";
import { OverviewGrid } from "@/features/admin/analytics/overview/overview-grid";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { getServerAnalyticsProductRankings, getServerAnalyticsTrends } from "@/lib/admin/server";
import { requireRole } from "@/lib/auth/server";

export default async function AnalyticsOverviewPage() {
  const user = await requireRole(["ADMIN", "SALES"], "/admin/analytics/overview");
  const [trends, rankings] = await Promise.all([
    getServerAnalyticsTrends({ granularity: "day" }).catch(() => null),
    getServerAnalyticsProductRankings({ range: "today", limit: 10 }).catch(() => null)
  ]);

  return (
    <AdminShell description="销售趋势、异常告警、商品排行与用户画像。" title="数据分析" user={user}>
      <AnalyticsShell active="overview">
        <OverviewGrid topProducts={rankings?.items ?? []} trends={trends} />
      </AnalyticsShell>
    </AdminShell>
  );
}
