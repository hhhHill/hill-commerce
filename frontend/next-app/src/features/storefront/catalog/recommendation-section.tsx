import { ProductCard } from "@/features/storefront/catalog/product-card";
import type { StorefrontProductCard } from "@/lib/storefront/types";

type RecommendationSectionProps = {
  products: StorefrontProductCard[];
  isLoading?: boolean;
};

export function RecommendationSection({ products, isLoading = false }: RecommendationSectionProps) {
  if (isLoading) {
    return (
      <section className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
        {Array.from({ length: 5 }).map((_, index) => (
          <div className="surface-card h-80 animate-pulse rounded-lg" key={index} />
        ))}
      </section>
    );
  }

  if (products.length === 0) {
    return null;
  }

  return (
    <section className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
      {products.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
    </section>
  );
}
