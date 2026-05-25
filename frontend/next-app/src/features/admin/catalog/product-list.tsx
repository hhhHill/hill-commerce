"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { deleteProduct, updateProductStatus } from "@/lib/admin/client";
import type { Category, ProductStatus, ProductListResult } from "@/lib/admin/types";

type ProductListProps = {
  result: ProductListResult;
  categories: Category[];
  filters: {
    name?: string;
    categoryId?: string;
    status?: string;
    page?: string;
  };
};

const STATUS_LABELS: Record<ProductStatus, string> = {
  DRAFT: "草稿",
  ON_SHELF: "已上架",
  OFF_SHELF: "已下架"
};

export function ProductList({ result, categories, filters }: ProductListProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleStatusChange(productId: number, status: ProductStatus) {
    setError("");
    startTransition(async () => {
      try {
        await updateProductStatus(productId, status);
        router.refresh();
      } catch (e) {
        setError(e instanceof Error ? e.message : "更新状态失败");
      }
    });
  }

  function handleDelete(productId: number) {
    if (!window.confirm("确认删除该商品？删除后可在数据库恢复。")) {
      return;
    }
    setError("");
    startTransition(async () => {
      try {
        await deleteProduct(productId);
        router.refresh();
      } catch (e) {
        setError(e instanceof Error ? e.message : "删除商品失败");
      }
    });
  }

  function buildPageHref(page: number) {
    const sp = new URLSearchParams();
    if (filters.name) sp.set("name", filters.name);
    if (filters.categoryId) sp.set("categoryId", filters.categoryId);
    if (filters.status) sp.set("status", filters.status);
    sp.set("page", String(page));
    return `/admin/products?${sp.toString()}`;
  }

  return (
    <div className="flex flex-col">
      {/* toolbar */}
      <div className="flex flex-wrap items-end gap-3 border-b border-[#f0f0f0] px-4 py-3">
        <form className="flex flex-wrap items-end gap-3">
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            关键词
            <input
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.name ?? ""}
              name="name"
              placeholder="名称 / SPU 编码"
            />
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            分类
            <select
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.categoryId ?? ""}
              name="categoryId"
            >
              <option value="">全部</option>
              {categories.map((c) => (
                <option key={c.id} value={String(c.id)}>{c.name}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            状态
            <select
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.status ?? ""}
              name="status"
            >
              <option value="">全部</option>
              <option value="DRAFT">草稿</option>
              <option value="ON_SHELF">已上架</option>
              <option value="OFF_SHELF">已下架</option>
            </select>
          </label>
          <button
            className="rounded-lg bg-[var(--brand-primary)] px-4 py-1.5 text-sm font-semibold text-white"
            type="submit"
          >
            查询
          </button>
        </form>
        <Link
          className="ml-auto rounded-lg bg-[var(--accent-strong)] px-4 py-1.5 text-sm font-semibold text-white"
          href="/admin/products/new"
        >
          ＋ 新建商品
        </Link>
      </div>

      {/* summary bar */}
      <div className="flex items-center justify-between border-b border-[#f0f0f0] px-4 py-2">
        <p className="text-sm text-[var(--text-secondary)]">
          共 <span className="font-semibold text-[var(--text-primary)]">{result.total}</span> 个商品
          {result.totalPages > 1 && (
            <span className="ml-2">第 {result.page}/{result.totalPages} 页</span>
          )}
        </p>
      </div>

      {error && (
        <div className="mx-4 mt-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
      )}

      {/* table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#f0f0f0] text-left text-xs text-[var(--text-hint)]">
              <th className="px-4 py-2.5 font-medium">商品名称</th>
              <th className="px-4 py-2.5 font-medium">SPU 编码</th>
              <th className="px-4 py-2.5 font-medium">分类</th>
              <th className="px-4 py-2.5 font-medium text-right">最低售价</th>
              <th className="px-4 py-2.5 font-medium">状态</th>
              <th className="px-4 py-2.5 font-medium text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            {result.items.map((product) => (
              <tr
                key={product.id}
                className="border-b border-[#f5f5f5] hover:bg-[#fafafa] transition-colors"
              >
                <td className="px-4 py-2.5">
                  <Link
                    className="font-medium text-[var(--text-primary)] hover:text-[var(--brand-primary)]"
                    href={`/admin/products/${product.id}`}
                  >
                    {product.name}
                  </Link>
                </td>
                <td className="px-4 py-2.5 font-mono text-xs text-[var(--text-secondary)]">
                  {product.spuCode}
                </td>
                <td className="px-4 py-2.5 text-[var(--text-secondary)]">
                  {product.categoryName}
                </td>
                <td className="px-4 py-2.5 text-right font-semibold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
                  {product.minSalePrice != null ? `¥${Number(product.minSalePrice).toFixed(2)}` : "-"}
                </td>
                <td className="px-4 py-2.5">
                  <span
                    className={`inline-flex rounded-[4px] px-1.5 py-0.5 text-xs font-medium ${
                      product.status === "ON_SHELF"
                        ? "bg-emerald-50 text-emerald-700"
                        : product.status === "DRAFT"
                          ? "bg-amber-50 text-amber-700"
                          : "bg-gray-100 text-gray-600"
                    }`}
                  >
                    {STATUS_LABELS[product.status]}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <Link
                      className="text-xs text-[var(--text-secondary)] hover:text-[var(--brand-primary)]"
                      href={`/admin/products/${product.id}`}
                    >
                      编辑
                    </Link>
                    <button
                      className="text-xs text-[var(--text-secondary)] hover:text-[var(--brand-primary)]"
                      disabled={isPending}
                      onClick={() =>
                        handleStatusChange(
                          product.id,
                          product.status === "ON_SHELF" ? "OFF_SHELF" : "ON_SHELF"
                        )
                      }
                      type="button"
                    >
                      {product.status === "ON_SHELF" ? "下架" : "上架"}
                    </button>
                    <button
                      className="text-xs text-[var(--text-hint)] hover:text-red-600"
                      disabled={isPending}
                      onClick={() => handleDelete(product.id)}
                      type="button"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* empty */}
      {result.items.length === 0 && (
        <p className="px-4 py-10 text-center text-sm text-[var(--text-hint)]">
          当前筛选条件下没有商品
        </p>
      )}

      {/* pagination */}
      {result.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-[#f0f0f0] px-4 py-2">
          <p className="text-xs text-[var(--text-hint)]">
            {result.page}/{result.totalPages} 页 · 共 {result.total} 个
          </p>
          <div className="flex gap-2">
            {result.page > 1 && (
              <Link
                className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={buildPageHref(result.page - 1)}
              >
                上一页
              </Link>
            )}
            {result.page < result.totalPages && (
              <Link
                className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={buildPageHref(result.page + 1)}
              >
                下一页
              </Link>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
