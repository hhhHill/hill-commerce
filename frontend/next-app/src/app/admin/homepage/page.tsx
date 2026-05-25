import { requireRole } from "@/lib/auth/server";
import { getAdminCategories } from "@/lib/admin/server";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { HomepageForm } from "@/features/admin/homepage/homepage-form";

export default async function AdminHomepagePage() {
  const user = await requireRole(["ADMIN"], "/admin/homepage");
  const categories = await getAdminCategories();

  return (
    <AdminShell
      description="管理首页的分类导航、活动卡片和展示顺序。"
      title="首页运营"
      user={user}
    >
      <HomepageForm categories={categories} />
    </AdminShell>
  );
}
