import { requireRole } from "@/lib/auth/server";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { ShopEditor } from "@/features/admin/shop/shop-editor";

export default async function ShopPage() {
  const user = await requireRole(["MERCHANT"], "/admin/shop");

  return (
    <AdminShell
      description="查看和编辑你的店铺基本信息。"
      title="我的店铺"
      user={user}
    >
      <ShopEditor />
    </AdminShell>
  );
}
