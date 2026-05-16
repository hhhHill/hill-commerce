import Link from "next/link";
import { notFound } from "next/navigation";

import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { ProductDetailPanel } from "@/features/storefront/catalog/product-detail";
import { ProductViewBeacon } from "@/features/storefront/catalog/product-view-beacon";
import { RecommendationSection } from "@/features/storefront/catalog/recommendation-section";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { getSessionUser } from "@/lib/auth/server";
import { StorefrontRequestError } from "@/lib/storefront/errors";
import { getServerStorefrontProductDetail, getServerStorefrontRecommendations } from "@/lib/storefront/server";

type ProductDetailPageProps = {
  params: Promise<{
    productId: string;
  }>;
};

export default async function ProductDetailPage({ params }: ProductDetailPageProps) {
  const { productId } = await params;
  try {
    const [product, user] = await Promise.all([getServerStorefrontProductDetail(Number(productId)), getSessionUser()]);
    const recommendations = await getServerStorefrontRecommendations({ type: "detail", productId: product.id, n: 6 });

    return (
      <main className="page-shell">
        <div className="page-stack">
          <ProductViewBeacon categoryId={product.categoryId} productId={product.id} />
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap gap-3">
              <HomeShortcut />
              <Link className="btn-secondary px-4 py-2" href="/categories">
                返回分类
              </Link>
            </div>
            <SearchForm />
          </div>
          <ProductDetailPanel
            isAuthenticated={Boolean(user)}
            loginHref={`/login?next=${encodeURIComponent(`/products/${product.id}`)}`}
            product={product}
          />
          <RecommendationSection products={recommendations} />
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
