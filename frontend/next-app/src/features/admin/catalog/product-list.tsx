"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { deleteProduct, updateProductStatus } from "@/lib/admin/client";
import type { Category, ProductStatus, ProductSummary } from "@/lib/admin/types";

type ProductListProps = {
  products: ProductSummary[];
  categories: Category[];
  filters: {
    name?: string;
    categoryId?: string;
    status?: string;
  };
};

const STATUS_LABELS: Record<ProductStatus, string> = {
  DRAFT: "草稿",
  ON_SHELF: "已上架",
  OFF_SHELF: "已下架"
};

export function ProductList({ products, categories, filters }: ProductListProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleStatusChange(productId: number, status: ProductStatus) {
    setError("");
    startTransition(async () => {
      try {
        await updateProductStatus(productId, status);
        router.refresh();
      } catch (submitError) {
        setError(submitError instanceof Error ? submitError.message : "更新状态失败");
      }
    });
  }

  function handleDelete(productId: number) {
    if (!window.confirm("逻辑删除后商品将不再出现在默认列表，确认继续吗？")) {
      return;
    }

    setError("");
    startTransition(async () => {
      try {
        await deleteProduct(productId);
        router.refresh();
      } catch (submitError) {
        setError(submitError instanceof Error ? submitError.message : "删除商品失败");
      }
    });
  }

  return (
    <div className="space-y-6">
      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <form className="grid gap-3 md:grid-cols-4">
            <input
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              defaultValue={filters.name ?? ""}
              name="name"
              placeholder="按商品名称搜索"
            />
            <select
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              defaultValue={filters.categoryId ?? ""}
              name="categoryId"
            >
              <option value="">全部分类</option>
              {categories.map((category) => (
                <option key={category.id} value={String(category.id)}>
                  {category.name}
                </option>
              ))}
            </select>
            <select
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              defaultValue={filters.status ?? ""}
              name="status"
            >
              <option value="">全部状态</option>
              <option value="DRAFT">草稿</option>
              <option value="ON_SHELF">已上架</option>
              <option value="OFF_SHELF">已下架</option>
            </select>
            <button className="rounded-2xl bg-[var(--accent)] px-5 py-3 font-semibold text-white" type="submit">
              过滤
            </button>
          </form>
          <Link className="rounded-full bg-[var(--accent-strong)] px-5 py-3 font-semibold text-white" href="/admin/products/new">
            新建商品
          </Link>
        </div>
        {error ? <p className="mt-4 rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p> : null}
      </section>

      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-semibold">商品列表</h2>
            <p className="mt-2 text-sm text-black/65">支持上下架、逻辑删除和进入分区式编辑页。</p>
          </div>
          <span className="rounded-full bg-[#f4e0cc] px-3 py-1 text-sm font-medium text-[var(--accent-strong)]">
            共 {products.length} 个
          </span>
        </div>
        <div className="mt-5 space-y-4">
          {products.map((product) => (
            <article
              key={product.id}
              className="grid gap-4 rounded-[24px] border border-black/10 bg-[#fffaf5] p-5 shadow-[0_10px_26px_rgba(29,20,13,0.04)] lg:grid-cols-[120px_minmax(0,1fr)_auto]"
            >
              <div className="overflow-hidden rounded-[20px] border border-black/5 bg-white">
                {product.coverImageUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img alt={product.name} className="h-full min-h-[120px] w-full object-cover" src={product.coverImageUrl} />
                ) : (
                  <div className="flex min-h-[120px] items-center justify-center text-sm text-black/40">无封面</div>
                )}
              </div>
              <div className="space-y-3">
                <div className="flex flex-wrap items-center gap-3">
                  <h3 className="text-xl font-semibold">{product.name}</h3>
                  <span className="rounded-full bg-white px-3 py-1 text-sm font-medium text-black/70">
                    {STATUS_LABELS[product.status]}
                  </span>
                </div>
                <dl className="grid gap-3 text-sm text-black/65 md:grid-cols-2">
                  <div>
                    <dt>分类</dt>
                    <dd className="mt-1 font-medium text-black/80">{product.categoryName}</dd>
                  </div>
                  <div>
                    <dt>SPU 编码</dt>
                    <dd className="mt-1 font-medium text-black/80">{product.spuCode}</dd>
                  </div>
                  <div>
                    <dt>最低售价</dt>
                    <dd className="mt-1 font-medium text-black/80">{product.minSalePrice ?? "-"}</dd>
                  </div>
                </dl>
              </div>
              <div className="flex flex-wrap gap-3 lg:w-[220px] lg:flex-col">
                <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href={`/admin/products/${product.id}`}>
                  编辑
                </Link>
                <button
                  className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium"
                  disabled={isPending}
                  onClick={() => handleStatusChange(product.id, product.status === "ON_SHELF" ? "OFF_SHELF" : "ON_SHELF")}
                  type="button"
                >
                  {product.status === "ON_SHELF" ? "下架" : "上架"}
                </button>
                <button
                  className="rounded-full border border-red-200 px-4 py-2 text-sm font-medium text-red-700"
                  disabled={isPending}
                  onClick={() => handleDelete(product.id)}
                  type="button"
                >
                  删除
                </button>
              </div>
            </article>
          ))}
          {products.length === 0 ? (
            <p className="rounded-[24px] border border-dashed border-black/10 px-5 py-10 text-center text-sm text-black/55">
              当前筛选条件下没有商品。你可以先创建商品，或者调整筛选条件。
            </p>
          ) : null}
        </div>
      </section>
    </div>
  );
}
