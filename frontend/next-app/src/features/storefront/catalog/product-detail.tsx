import type { StorefrontProductDetail } from "@/lib/storefront/types";
import { AddToCartPanel } from "@/features/storefront/cart/add-to-cart-panel";

type ProductDetailPanelProps = {
  product: StorefrontProductDetail;
  isAuthenticated: boolean;
  loginHref: string;
};

export function ProductDetailPanel({ product, isAuthenticated, loginHref }: ProductDetailPanelProps) {
  return (
    <section className="mx-auto flex w-full max-w-2xl flex-col gap-4">
      <div className="flex flex-col gap-4">
        <div className="surface-card overflow-hidden rounded-lg">
          <div className="aspect-[4/5] bg-[var(--border-light)]">
            {product.coverImageUrl ? (
              <img alt={product.name} className="h-full w-full object-cover" src={product.coverImageUrl} />
            ) : (
              <div className="flex h-full items-center justify-center text-sm font-medium text-[var(--text-hint)]">暂无商品图片</div>
            )}
          </div>
        </div>
        {product.detailImages.length > 0 ? (
          <div className="grid grid-cols-3 gap-2">
            {product.detailImages.map((imageUrl) => (
              <div key={imageUrl} className="overflow-hidden rounded-lg border border-[var(--border-normal)] bg-white">
                <img alt={product.name} className="aspect-square w-full object-cover" src={imageUrl} />
              </div>
            ))}
          </div>
        ) : null}
      </div>

      <div className="surface-card flex flex-col gap-4 rounded-lg px-4 py-4">
        <div className="flex flex-col gap-3">
          <div className="flex max-w-xl flex-col gap-2">
            <span className="chip-badge w-fit">
              {product.categoryName}
            </span>
            <h1 className="text-[28px] font-semibold tracking-tight">{product.name}</h1>
            {product.subtitle ? <p className="text-sm leading-6 text-[var(--text-secondary)]">{product.subtitle}</p> : null}
          </div>
          <div className="surface-subtle px-4 py-4">
            <p className="text-sm text-[var(--text-secondary)]">到手价</p>
            <p className="mt-2 text-[32px] font-bold leading-none tracking-tight text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
              {product.salePrice ? `¥${product.salePrice}` : "待定价"}
            </p>
          </div>
        </div>

        <div className="grid gap-2 sm:grid-cols-2">
          <InfoTile label="可售状态" value={renderSaleStatus(product.saleStatus)} />
          <InfoTile label="SKU 数量" value={`${product.skus.length} 个可查看选项`} />
        </div>

        <AddToCartPanel isAuthenticated={isAuthenticated} loginHref={loginHref} product={product} />

        {product.salesAttributes.length > 0 ? (
          <section className="flex flex-col gap-3">
            <h2 className="text-sm font-semibold text-[var(--text-secondary)]">规格选项</h2>
            <div className="flex flex-col gap-3">
              {product.salesAttributes.map((attribute) => (
                <div key={attribute.id} className="surface-subtle px-4 py-4">
                  <p className="text-sm font-semibold">{attribute.name}</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {attribute.values.map((value) => (
                      <span key={value.id} className="rounded-md border border-[var(--border-normal)] bg-white px-3 py-2 text-sm text-[var(--text-secondary)]">
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
          <h2 className="text-sm font-semibold text-[var(--text-secondary)]">SKU 明细</h2>
          <div className="flex flex-col gap-3">
            {product.skus.map((sku) => (
              <div key={sku.id} className="surface-subtle px-4 py-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold">{sku.salesAttrValueText}</p>
                    <p className="mt-1 text-sm text-[var(--text-secondary)]">{sku.skuCode}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-bold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
                      ¥{sku.price}
                    </p>
                    <p className="text-sm text-[var(--text-secondary)]">{renderStockStatus(sku.stockStatus, sku.stock)}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {product.description ? (
          <section className="flex flex-col gap-3">
            <h2 className="text-sm font-semibold text-[var(--text-secondary)]">商品详情</h2>
            <div
              className="surface-subtle px-4 py-4 text-sm leading-7 text-[var(--text-secondary)]"
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
    <div className="surface-subtle px-4 py-4">
      <p className="text-sm text-[var(--text-secondary)]">{label}</p>
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
