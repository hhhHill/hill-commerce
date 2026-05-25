import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { ProductList } from "@/features/admin/catalog/product-list";
import { requireRole } from "@/lib/auth/server";
import { getAdminCategories, getAdminProducts } from "@/lib/admin/server";

type SearchParams = {
  name?: string;
  categoryId?: string;
  status?: string;
  page?: string;
};

export default async function AdminProductsPage({
  searchParams,
}: {
  searchParams: Promise<SearchParams>;
}) {
  const params = await searchParams;
  const user = await requireRole(["ADMIN", "MERCHANT"], "/admin/products");
  const filters = {
    name: params.name,
    categoryId: params.categoryId,
    status: params.status,
  };
  const page = Number(params.page) || 1;
  const size = 20;

  const [result, categories] = await Promise.all([
    getAdminProducts(filters, page, size),
    getAdminCategories(),
  ]);

  return (
    <AdminShell
      description="管理所有商品，支持搜索、筛选和上下架操作。"
      title="商品管理"
      user={user}
    >
      <ProductList
        categories={categories}
        filters={{ ...filters, page: params.page }}
        result={result}
      />
    </AdminShell>
  );
}
