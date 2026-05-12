import { BrowseEventLink } from "@/features/storefront/catalog/browse-event-link";
import { STOREFRONT_BROWSE_EVENTS } from "@/lib/storefront/logging";
import type { StorefrontProductCard } from "@/lib/storefront/types";

type ProductCardProps = {
  product: StorefrontProductCard;
};

export function ProductCard({ product }: ProductCardProps) {
  return (
    <BrowseEventLink
      className="group flex h-full flex-col overflow-hidden rounded-[28px] border border-black/10 bg-white/90 shadow-[0_16px_40px_rgba(74,42,18,0.08)] transition-transform duration-200 hover:-translate-y-1"
      eventName={STOREFRONT_BROWSE_EVENTS.productClick}
      eventPayload={{ categoryId: product.categoryId, productId: product.id, source: "product-list-card" }}
      href={`/products/${product.id}`}
    >
      <div className="relative aspect-[4/5] overflow-hidden bg-[linear-gradient(160deg,#f4e7d2_0%,#e7d1ba_100%)]">
        {product.coverImageUrl ? (
          <img
            alt={product.name}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.03]"
            src={product.coverImageUrl}
          />
        ) : (
          <div className="flex h-full items-center justify-center text-sm font-medium text-black/40">暂无商品图片</div>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-3 px-5 py-5">
        <h3 className="line-clamp-2 text-lg font-semibold leading-6 tracking-tight">{product.name}</h3>
        <div className="mt-auto flex items-end justify-between gap-3">
          <p className="text-2xl font-semibold tracking-tight text-[var(--accent-strong)]">
            {product.salePrice ? `¥${product.salePrice}` : "待定价"}
          </p>
          <span className="rounded-full border border-black/10 px-3 py-1 text-xs font-medium text-black/55">查看详情</span>
        </div>
      </div>
    </BrowseEventLink>
  );
}
