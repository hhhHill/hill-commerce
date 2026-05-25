import { BrowseEventLink } from "@/features/storefront/catalog/browse-event-link";
import { buildProductCardViewModel } from "@/features/storefront/catalog/product-card-view-model";
import { STOREFRONT_BROWSE_EVENTS } from "@/lib/storefront/logging";
import type { StorefrontProductCard } from "@/lib/storefront/types";

type ProductCardProps = {
  product: StorefrontProductCard;
};

export function ProductCard({ product }: ProductCardProps) {
  const view = buildProductCardViewModel(product);

  return (
    <BrowseEventLink
      className="group flex h-full flex-col overflow-hidden rounded-xl bg-white transition-colors hover:bg-[#fffaf5]"
      eventName={STOREFRONT_BROWSE_EVENTS.productClick}
      eventPayload={{ categoryId: product.categoryId, productId: product.id, source: "product-list-card" }}
      href={`/products/${product.id}`}
    >
      <div className="relative aspect-square overflow-hidden bg-[var(--border-light)]">
        {product.coverImageUrl ? (
          <img
            alt={product.name}
            className="h-full w-full object-cover"
            src={product.coverImageUrl}
          />
        ) : (
          <div className="flex h-full items-center justify-center px-4 text-center text-xs font-medium text-[var(--text-hint)]">
            暂无商品图片
          </div>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-1.5 px-2.5 py-2.5">
        <div className="flex min-h-5 flex-wrap gap-1 overflow-hidden">
          {view.tags.slice(0, 3).map((tag) => (
            <span
              className="rounded-[4px] bg-[#fff2ec] px-1.5 py-0.5 text-[10px] font-semibold leading-none text-[var(--brand-primary)]"
              key={tag}
            >
              {tag}
            </span>
          ))}
        </div>
        <span className="line-clamp-2 min-h-[40px] text-sm leading-[20px] text-[var(--text-primary)]">
          {product.name}
        </span>
        <div className="flex min-w-0 items-end gap-1.5">
          {product.salePrice ? (
            <>
              <p className="min-w-0 leading-none text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
                <span className="align-baseline text-sm font-bold">¥</span>
                <span className="text-[22px] font-extrabold tracking-normal">{view.price}</span>
              </p>
              {view.originalPrice ? (
                <span className="pb-0.5 text-xs leading-none text-[var(--price-strike)] line-through">
                  ¥{view.originalPrice}
                </span>
              ) : null}
            </>
          ) : (
            <p className="text-lg font-bold leading-none text-[var(--price)]">{view.price}</p>
          )}
        </div>
        <div className="flex min-w-0 items-center gap-1 text-xs leading-4 text-[var(--text-hint)]">
          <span className="shrink-0">{view.sales}</span>
          <span className="shrink-0">·</span>
          <span className="truncate">{view.shopName}</span>
        </div>
      </div>
    </BrowseEventLink>
  );
}
