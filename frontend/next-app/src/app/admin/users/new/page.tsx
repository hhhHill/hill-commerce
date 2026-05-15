import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AdminUserForm } from "@/features/admin/user/admin-user-form";
import { requireRole } from "@/lib/auth/server";

export default async function NewAdminUserPage() {
  const user = await requireRole(["ADMIN"], "/admin/users/new");

  return (
    <AdminShell
      description="为新的销售同学开通后台账号，创建成功后可直接使用邮箱和初始密码登录。"
      title="新增 Sales"
      user={user}
    >
      <AdminUserForm />
    </AdminShell>
  );
}
