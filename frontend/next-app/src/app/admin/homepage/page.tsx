import { requireRole } from "@/lib/auth/server";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { HomepageForm } from "@/features/admin/homepage/homepage-form";

export default async function AdminHomepagePage() {
  const user = await requireRole(["ADMIN", "MERCHANT"], "/admin/homepage");

  return (
    <AdminShell
      description="管理首页 2×2 活动卡片的标题、图片和跳转链接。"
      title="首页运营"
      user={user}
    >
      <HomepageForm />
    </AdminShell>
  );
}
