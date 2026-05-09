import { redirect } from "next/navigation";

import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { ProductEditor } from "@/features/admin/catalog/product-editor";
import { requireRole } from "@/lib/auth/server";
import { getAdminCategories } from "@/lib/admin/server";

export default async function NewAdminProductPage() {
  const user = await requireRole(["ADMIN", "SALES"], "/admin/products/new");
  const categories = await getAdminCategories();

  if (categories.length === 0) {
    redirect("/admin/categories");
  }

  return (
    <AdminShell
      description="按基础信息、图片与描述、属性和 SKU 四个分区录入，但最终一次性聚合保存。"
      title="新建商品"
      user={user}
    >
      <ProductEditor categories={categories} />
    </AdminShell>
  );
}
