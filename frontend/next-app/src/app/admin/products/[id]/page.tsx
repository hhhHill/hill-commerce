import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { ProductEditor } from "@/features/admin/catalog/product-editor";
import { requireRole } from "@/lib/auth/server";
import { getAdminCategories, getAdminProduct } from "@/lib/admin/server";

type EditAdminProductPageProps = {
  params: Promise<{
    id: string;
  }>;
};

export default async function EditAdminProductPage({ params }: EditAdminProductPageProps) {
  const { id } = await params;
  const user = await requireRole(["ADMIN", "SALES"], `/admin/products/${id}`);
  const productId = Number(id);
  const [categories, product] = await Promise.all([getAdminCategories(), getAdminProduct(productId)]);

  return (
    <AdminShell
      description="编辑页会回填当前商品的图片、展示属性、销售属性和 SKU，保存时覆盖写入。"
      title={`编辑商品 #${product.id}`}
      user={user}
    >
      <ProductEditor categories={categories} product={product} />
    </AdminShell>
  );
}
