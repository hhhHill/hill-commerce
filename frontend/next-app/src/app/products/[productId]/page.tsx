import Link from "next/link";
import { notFound } from "next/navigation";

import { ProductDetailPanel } from "@/features/storefront/catalog/product-detail";
import { ProductViewBeacon } from "@/features/storefront/catalog/product-view-beacon";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { getSessionUser } from "@/lib/auth/server";
import { StorefrontRequestError } from "@/lib/storefront/errors";
import { getServerStorefrontProductDetail } from "@/lib/storefront/server";

type ProductDetailPageProps = {
  params: Promise<{
    productId: string;
  }>;
};

export default async function ProductDetailPage({ params }: ProductDetailPageProps) {
  const { productId } = await params;
  try {
    const [product, user] = await Promise.all([getServerStorefrontProductDetail(Number(productId)), getSessionUser()]);

    return (
      <main className="min-h-screen px-6 py-10">
        <div className="mx-auto flex max-w-6xl flex-col gap-8">
          <ProductViewBeacon categoryId={product.categoryId} productId={product.id} />
          <div className="flex flex-wrap items-center justify-between gap-3">
            <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/categories">
              返回分类
            </Link>
            <SearchForm className="w-full max-w-md" />
          </div>
          <ProductDetailPanel
            isAuthenticated={Boolean(user)}
            loginHref={`/login?next=${encodeURIComponent(`/products/${product.id}`)}`}
            product={product}
          />
        </div>
      </main>
    );
  } catch (error) {
    if (error instanceof StorefrontRequestError && error.status === 404) {
      notFound();
    }

    throw error;
  }
}
