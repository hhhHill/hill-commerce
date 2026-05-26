import { Suspense } from "react";

import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AdminUserList } from "@/features/admin/user/admin-user-list";
import { requireRole } from "@/lib/auth/server";
import { getServerMerchantUsers, getServerLoginLogs } from "@/lib/admin/server";

type AdminUsersPageProps = {
  searchParams: Promise<{
    tab?: string;
    email?: string;
    result?: string;
  }>;
};

export default async function AdminUsersPage({ searchParams }: AdminUsersPageProps) {
  const user = await requireRole(["ADMIN"], "/admin/users");
  const { email, result } = await searchParams;

  const [users, loginLogs] = await Promise.all([
    getServerMerchantUsers(),
    getServerLoginLogs({ email, result }),
  ]);

  return (
    <AdminShell
      description="集中查看和管理 Merchant 账户，支持新增、重置密码和逻辑禁用。"
      title="用户管理"
      user={user}
    >
      <Suspense fallback={null}>
        <AdminUserList
          users={users}
          loginLogs={loginLogs}
          loginFilters={{ email, result }}
        />
      </Suspense>
    </AdminShell>
  );
}
