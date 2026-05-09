import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { ProductList } from "@/features/admin/catalog/product-list";
import { requireRole } from "@/lib/auth/server";
import { getAdminCategories, getAdminProducts } from "@/lib/admin/server";

type ProductListPageProps = {
  searchParams: Promise<{
    name?: string;
    categoryId?: string;
    status?: string;
  }>;
};

export default async function AdminProductsPage({ searchParams }: ProductListPageProps) {
  const user = await requireRole(["ADMIN", "SALES"], "/admin/products");
  const filters = await searchParams;
  const [categories, products] = await Promise.all([
    getAdminCategories(),
    getAdminProducts(filters)
  ]);

  return (
    <AdminShell
      description="商品列表支持按名称、分类、状态过滤，并可直接执行上下架和逻辑删除。"
      title="商品管理"
      user={user}
    >
      <ProductList categories={categories} filters={filters} products={products} />
    </AdminShell>
  );
}
