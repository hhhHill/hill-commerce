import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AdminUserList } from "@/features/admin/user/admin-user-list";
import { requireRole } from "@/lib/auth/server";
import { getServerSalesUsers } from "@/lib/admin/server";

export default async function AdminUsersPage() {
  const user = await requireRole(["ADMIN"], "/admin/users");
  const users = await getServerSalesUsers();

  return (
    <AdminShell
      description="集中查看和管理 Sales 账户，支持新增、重置密码和逻辑禁用。"
      title="用户管理"
      user={user}
    >
      <AdminUserList users={users} />
    </AdminShell>
  );
}
