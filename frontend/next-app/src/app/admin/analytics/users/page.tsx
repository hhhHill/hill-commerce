import { AnalyticsShell } from "@/features/admin/analytics/analytics-shell";
import { ClientUserAnalytics } from "@/features/admin/analytics/users/client-user-analytics";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { getServerAnalyticsAggregateProfiles } from "@/lib/admin/server";
import { requireRole } from "@/lib/auth/server";

export default async function AnalyticsUsersPage() {
  const user = await requireRole(["ADMIN"], "/admin/analytics/users");
  const aggregate = await getServerAnalyticsAggregateProfiles().catch(() => null);

  return (
    <AdminShell description="群体画像、购买力分层与个体用户分析。" title="用户画像" user={user}>
      <AnalyticsShell active="users">
        <ClientUserAnalytics aggregate={aggregate} />
      </AnalyticsShell>
    </AdminShell>
  );
}
