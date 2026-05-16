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
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        </div>
        <div className="chip-badge">
          共 {total} 件商品
        </div>
      </div>

      {products.length === 0 ? (
        <EmptyState description={emptyDescription} title="暂时没有可展示商品" />
      ) : (
        <>
          <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
          {buildPageHref ? (
            <div className="surface-card flex flex-wrap items-center justify-between gap-3 rounded-lg px-4 py-3">
              <p className="text-sm text-[var(--text-secondary)]">
                第 {page} / {totalPages} 页
              </p>
              <div className="flex gap-3">
                {canGoPrevious ? (
                  <Link className="btn-secondary px-4 py-2" href={buildPageHref(page - 1)}>
                    上一页
                  </Link>
                ) : null}
                {canGoNext ? (
                  <Link className="btn-primary px-4 py-2" href={buildPageHref(page + 1)}>
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
