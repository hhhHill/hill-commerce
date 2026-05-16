import { BrowseEventLink } from "@/features/storefront/catalog/browse-event-link";
import { STOREFRONT_BROWSE_EVENTS } from "@/lib/storefront/logging";
import type { StorefrontProductCard } from "@/lib/storefront/types";

type ProductCardProps = {
  product: StorefrontProductCard;
};

export function ProductCard({ product }: ProductCardProps) {
  return (
    <div className="surface-card group flex h-full flex-col overflow-hidden rounded-lg">
      <BrowseEventLink
        className="flex flex-1 flex-col"
        eventName={STOREFRONT_BROWSE_EVENTS.productClick}
        eventPayload={{ categoryId: product.categoryId, productId: product.id, source: "product-list-card" }}
        href={`/products/${product.id}`}
      >
        <div className="relative aspect-[4/5] overflow-hidden bg-[var(--border-light)]">
          {product.coverImageUrl ? (
            <img
              alt={product.name}
              className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.03]"
              src={product.coverImageUrl}
            />
          ) : (
            <div className="flex h-full items-center justify-center px-4 text-center text-sm font-medium text-[var(--text-hint)]">暂无商品图片</div>
          )}
        </div>
      </BrowseEventLink>
      <div className="flex flex-1 flex-col gap-3 p-3">
        <BrowseEventLink
          className="line-clamp-2 min-h-10 text-[13px] font-medium leading-5 text-[var(--text-primary)]"
          eventName={STOREFRONT_BROWSE_EVENTS.productClick}
          eventPayload={{ categoryId: product.categoryId, productId: product.id, source: "product-list-name" }}
          href={`/products/${product.id}`}
        >
          {product.name}
        </BrowseEventLink>
        <p className="text-xl font-bold leading-none text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
          {product.salePrice ? `¥${product.salePrice}` : "待定价"}
        </p>
        <BrowseEventLink
          className="btn-primary mt-auto w-full py-2.5"
          eventName={STOREFRONT_BROWSE_EVENTS.productClick}
          eventPayload={{ categoryId: product.categoryId, productId: product.id, source: "product-list-cta" }}
          href={`/products/${product.id}`}
        >
          马上抢
        </BrowseEventLink>
      </div>
    </div>
  );
}
