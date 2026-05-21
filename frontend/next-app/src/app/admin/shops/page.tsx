import { requireRole } from "@/lib/auth/server";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { ShopList } from "@/features/admin/shop/shop-list";

export default async function ShopsPage() {
  const user = await requireRole(["ADMIN"], "/admin/shops");

  return (
    <AdminShell
      description="集中管理所有店铺，支持启用和停用操作。"
      title="店铺管理"
      user={user}
    >
      <ShopList />
    </AdminShell>
  );
}
