import type { StorefrontProductDetail } from "@/lib/storefront/types";
import { AddToCartPanel } from "@/features/storefront/cart/add-to-cart-panel";

type ProductDetailPanelProps = {
  product: StorefrontProductDetail;
  isAuthenticated: boolean;
  loginHref: string;
};

export function ProductDetailPanel({ product, isAuthenticated, loginHref }: ProductDetailPanelProps) {
  return (
    <section className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
      <div className="flex flex-col gap-4">
        <div className="overflow-hidden rounded-[32px] border border-black/10 bg-white/90 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
          <div className="aspect-[4/5] bg-[linear-gradient(160deg,#f4e7d2_0%,#e7d1ba_100%)]">
            {product.coverImageUrl ? (
              <img alt={product.name} className="h-full w-full object-cover" src={product.coverImageUrl} />
            ) : (
              <div className="flex h-full items-center justify-center text-sm font-medium text-black/40">暂无商品图片</div>
            )}
          </div>
        </div>
        {product.detailImages.length > 0 ? (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
            {product.detailImages.map((imageUrl) => (
              <div key={imageUrl} className="overflow-hidden rounded-[22px] border border-black/10 bg-white/80">
                <img alt={product.name} className="aspect-square w-full object-cover" src={imageUrl} />
              </div>
            ))}
          </div>
        ) : null}
      </div>

      <div className="flex flex-col gap-6 rounded-[32px] border border-black/10 bg-white/85 px-6 py-7 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex max-w-xl flex-col gap-3">
            <span className="w-fit rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
              {product.categoryName}
            </span>
            <h1 className="text-4xl font-semibold tracking-tight">{product.name}</h1>
            {product.subtitle ? <p className="text-base leading-7 text-black/65">{product.subtitle}</p> : null}
          </div>
          <div className="rounded-[24px] bg-[var(--surface)] px-5 py-4 text-right">
            <p className="text-sm text-black/50">售价</p>
            <p className="text-3xl font-semibold tracking-tight text-[var(--accent-strong)]">
              {product.salePrice ? `¥${product.salePrice}` : "待定价"}
            </p>
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <InfoTile label="可售状态" value={renderSaleStatus(product.saleStatus)} />
          <InfoTile label="SKU 数量" value={`${product.skus.length} 个可查看选项`} />
        </div>

        <AddToCartPanel isAuthenticated={isAuthenticated} loginHref={loginHref} product={product} />

        {product.salesAttributes.length > 0 ? (
          <section className="flex flex-col gap-3">
            <h2 className="text-sm font-semibold uppercase tracking-[0.18em] text-black/45">规格选项</h2>
            <div className="flex flex-col gap-3">
              {product.salesAttributes.map((attribute) => (
                <div key={attribute.id} className="rounded-[24px] border border-black/10 bg-[var(--surface)] px-4 py-4">
                  <p className="text-sm font-semibold">{attribute.name}</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {attribute.values.map((value) => (
                      <span key={value.id} className="rounded-full border border-black/10 bg-white px-3 py-1 text-sm text-black/70">
                        {value.value}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>
        ) : null}

        <section className="flex flex-col gap-3">
          <h2 className="text-sm font-semibold uppercase tracking-[0.18em] text-black/45">SKU 明细</h2>
          <div className="flex flex-col gap-3">
            {product.skus.map((sku) => (
              <div key={sku.id} className="rounded-[24px] border border-black/10 bg-[var(--surface)] px-4 py-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold">{sku.salesAttrValueText}</p>
                    <p className="mt-1 text-sm text-black/55">{sku.skuCode}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-semibold text-[var(--accent-strong)]">¥{sku.price}</p>
                    <p className="text-sm text-black/55">{renderStockStatus(sku.stockStatus, sku.stock)}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {product.description ? (
          <section className="flex flex-col gap-3">
            <h2 className="text-sm font-semibold uppercase tracking-[0.18em] text-black/45">商品详情</h2>
            <div
              className="rounded-[24px] border border-black/10 bg-[var(--surface)] px-4 py-4 text-sm leading-7 text-black/70"
              dangerouslySetInnerHTML={{ __html: product.description }}
            />
          </section>
        ) : null}
      </div>
    </section>
  );
}

function InfoTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[24px] border border-black/10 bg-[var(--surface)] px-4 py-4">
      <p className="text-sm text-black/50">{label}</p>
      <p className="mt-2 text-lg font-semibold">{value}</p>
    </div>
  );
}

function renderSaleStatus(status: StorefrontProductDetail["saleStatus"]): string {
  switch (status) {
    case "AVAILABLE":
      return "当前可售";
    case "OUT_OF_STOCK":
      return "库存紧张";
    case "UNAVAILABLE":
      return "暂不可售";
    case "OFF_SHELF":
      return "已下架，仅供浏览";
    default:
      return status;
  }
}

function renderStockStatus(status: StorefrontProductDetail["skus"][number]["stockStatus"], stock: number): string {
  switch (status) {
    case "IN_STOCK":
      return `库存充足 · ${stock}`;
    case "LOW_STOCK":
      return `库存偏低 · ${stock}`;
    case "OUT_OF_STOCK":
      return "暂时缺货";
    default:
      return String(stock);
  }
}
