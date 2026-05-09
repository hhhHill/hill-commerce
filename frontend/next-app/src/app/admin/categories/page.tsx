import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { CategoryManager } from "@/features/admin/catalog/category-manager";
import { requireRole } from "@/lib/auth/server";
import { getAdminCategories } from "@/lib/admin/server";

export default async function AdminCategoriesPage() {
  const user = await requireRole(["ADMIN", "SALES"], "/admin/categories");
  const categories = await getAdminCategories();

  return (
    <AdminShell
      description="一级分类只维护名称、排序和启停状态。停用分类不会被新商品继续使用。"
      title="分类管理"
      user={user}
    >
      <CategoryManager categories={categories} />
    </AdminShell>
  );
}
