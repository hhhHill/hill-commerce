import Link from "next/link";

import { EmptyState } from "@/features/storefront/catalog/empty-state";
import { ProductCard } from "@/features/storefront/catalog/product-card";
import type { StorefrontProductCard } from "@/lib/storefront/types";

type StorefrontProductListProps = {
  title: string;
  products: StorefrontProductCard[];
  emptyDescription: string;
  page?: number;
  pageSize?: number;
  total?: number;
  buildPageHref?: (page: number) => string;
};

export function StorefrontProductList({
  title,
  products,
  emptyDescription,
  page = 1,
  pageSize = 12,
  total = products.length,
  buildPageHref
}: StorefrontProductListProps) {
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const canGoPrevious = page > 1;
  const canGoNext = page < totalPages;

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-2">
          <span className="text-xs font-semibold uppercase tracking-[0.22em] text-black/45">Storefront Listing</span>
          <h1 className="text-4xl font-semibold tracking-tight">{title}</h1>
        </div>
        <div className="rounded-full border border-black/10 bg-white/70 px-4 py-2 text-sm text-black/60">
          共 {total} 件商品
        </div>
      </div>

      {products.length === 0 ? (
        <EmptyState description={emptyDescription} title="暂时没有可展示商品" />
      ) : (
        <>
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
          {buildPageHref ? (
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-[24px] border border-black/10 bg-white/70 px-5 py-4">
              <p className="text-sm text-black/55">
                第 {page} / {totalPages} 页
              </p>
              <div className="flex gap-3">
                {canGoPrevious ? (
                  <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href={buildPageHref(page - 1)}>
                    上一页
                  </Link>
                ) : null}
                {canGoNext ? (
                  <Link className="rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white" href={buildPageHref(page + 1)}>
                    下一页
                  </Link>
                ) : null}
              </div>
            </div>
          ) : null}
        </>
      )}
    </section>
  );
}
