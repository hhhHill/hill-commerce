"use client";

import Link from "next/link";
import { useState } from "react";
import type { ProductLogEntry, ProductLogListResult } from "@/lib/admin/types";

const ACTION_TYPE_OPTIONS = [
  { value: "", label: "全部操作" },
  { value: "CREATE_PRODUCT", label: "创建商品" },
  { value: "UPDATE_PRODUCT", label: "更新商品" },
  { value: "UPDATE_PRODUCT_STATUS", label: "上下架" },
  { value: "DELETE_PRODUCT", label: "删除商品" },
];

type AdminProductLogCenterProps = {
  result: ProductLogListResult;
  filters: {
    actionType?: string;
    productName?: string;
    spuCode?: string;
    operatorUserId?: string;
    page?: string;
  };
};

export function AdminProductLogCenter({ result, filters }: AdminProductLogCenterProps) {
  const [expandedId, setExpandedId] = useState<number | null>(null);

  return (
    <div className="flex flex-col">
      {/* toolbar */}
      <div className="flex flex-wrap items-end gap-3 border-b border-[#f0f0f0] px-4 py-3">
        <form className="flex flex-wrap items-end gap-3">
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            商品名称
            <input
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.productName ?? ""}
              name="productName"
              placeholder="模糊搜索"
            />
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            SPU 编码
            <input
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.spuCode ?? ""}
              name="spuCode"
              placeholder="精确匹配"
            />
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            操作类型
            <select
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.actionType ?? ""}
              name="actionType"
            >
              {ACTION_TYPE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            操作者 ID
            <input
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.operatorUserId ?? ""}
              name="operatorUserId"
              placeholder="用户 ID"
            />
          </label>
          <button
            className="rounded-lg bg-[var(--brand-primary)] px-4 py-1.5 text-sm font-semibold text-white"
            type="submit"
          >
            查询
          </button>
        </form>
      </div>

      {/* summary bar */}
      <div className="flex items-center justify-between border-b border-[#f0f0f0] px-4 py-2">
        <p className="text-sm text-[var(--text-secondary)]">
          共 <span className="font-semibold text-[var(--text-primary)]">{result.total}</span> 条记录
          {result.totalPages > 1 && (
            <span className="ml-2">第 {result.page}/{result.totalPages} 页</span>
          )}
        </p>
      </div>

      {/* table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#f0f0f0] text-left text-xs text-[var(--text-hint)]">
              <th className="px-4 py-2.5 font-medium">商品名称</th>
              <th className="px-4 py-2.5 font-medium">SPU 编码</th>
              <th className="px-4 py-2.5 font-medium">操作类型</th>
              <th className="px-4 py-2.5 font-medium">变更详情</th>
              <th className="px-4 py-2.5 font-medium">操作者</th>
              <th className="px-4 py-2.5 font-medium">时间</th>
              <th className="px-4 py-2.5 font-medium">IP</th>
            </tr>
          </thead>
          <tbody>
            {result.items.map((item) => (
              <ProductLogRow
                key={item.id}
                item={item}
                isExpanded={expandedId === item.id}
                onToggleExpand={() =>
                  setExpandedId(expandedId === item.id ? null : item.id)
                }
              />
            ))}
          </tbody>
        </table>
      </div>

      {/* empty */}
      {result.items.length === 0 && (
        <p className="px-4 py-10 text-center text-sm text-[var(--text-hint)]">
          {result.total === 0
            ? "暂无商品操作记录，商品创建、编辑、删除后会自动记录"
            : "当前筛选条件下没有商品操作记录"}
        </p>
      )}

      {/* pagination */}
      {result.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-[#f0f0f0] px-4 py-2">
          <p className="text-xs text-[var(--text-hint)]">
            {result.page}/{result.totalPages} 页 · 共 {result.total} 条
          </p>
          <div className="flex gap-2">
            {result.page > 1 && (
              <Link
                className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={buildPageHref(filters, result.page - 1)}
              >
                上一页
              </Link>
            )}
            {result.page < result.totalPages && (
              <Link
                className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={buildPageHref(filters, result.page + 1)}
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

function ProductLogRow({
  item,
  isExpanded,
  onToggleExpand,
}: {
  item: ProductLogEntry;
  isExpanded: boolean;
  onToggleExpand: () => void;
}) {
  const hasChanges = item.fieldChanges && Object.keys(item.fieldChanges).length > 0;

  return (
    <>
      <tr
        className={`border-b border-[#f5f5f5] hover:bg-[#fafafa] transition-colors ${
          hasChanges ? "cursor-pointer" : ""
        }`}
        onClick={() => hasChanges && onToggleExpand()}
        role={hasChanges ? "button" : undefined}
        tabIndex={hasChanges ? 0 : undefined}
        onKeyDown={(e) => {
          if (hasChanges && (e.key === "Enter" || e.key === " ")) {
            e.preventDefault();
            onToggleExpand();
          }
        }}
      >
        <td className="px-4 py-2.5">
          <Link
            className="font-medium text-[var(--text-primary)] hover:text-[var(--brand-primary)]"
            href={`/admin/products/${item.targetId}`}
            onClick={(e) => e.stopPropagation()}
          >
            {item.targetName ?? "—"}
          </Link>
        </td>
        <td className="px-4 py-2.5 font-mono text-xs text-[var(--text-secondary)]">
          {item.targetSpuCode ?? "—"}
        </td>
        <td className="px-4 py-2.5">
          <span
            className={`inline-flex rounded-[4px] px-1.5 py-0.5 text-xs font-medium ${
              item.actionType === "CREATE_PRODUCT"
                ? "bg-emerald-50 text-emerald-700"
                : item.actionType === "DELETE_PRODUCT"
                  ? "bg-red-50 text-red-700"
                  : "bg-blue-50 text-blue-700"
            }`}
          >
            {renderActionType(item.actionType)}
          </span>
        </td>
        <td className="px-4 py-2.5 max-w-[300px] text-[var(--text-secondary)]">
          {summarizeChanges(item.fieldChanges)}
        </td>
        <td className="px-4 py-2.5 text-[var(--text-secondary)]">
          {item.operatorUserId} / {item.operatorRole}
        </td>
        <td className="px-4 py-2.5 whitespace-nowrap text-[var(--text-hint)]">
          {formatDateTime(item.createdAt)}
        </td>
        <td className="px-4 py-2.5 font-mono text-xs text-[var(--text-hint)]">
          {item.ipAddress}
        </td>
      </tr>
      {isExpanded && hasChanges && (
        <tr>
          <td colSpan={7} className="bg-[#fafafa] px-4 py-3 border-b border-[#f0f0f0]">
            <div className="text-sm space-y-1">
              <p className="font-medium text-[var(--text-primary)] mb-2">变更详情</p>
              {Object.entries(item.fieldChanges!).map(([field, change]) => (
                <div key={field} className="flex gap-4">
                  <span className="text-[var(--text-hint)] w-20 shrink-0">
                    {renderFieldName(field)}
                  </span>
                  <span className="text-red-500 line-through">
                    {formatFieldValue(field, change.old)}
                  </span>
                  <span className="text-[var(--text-secondary)]">→</span>
                  <span className="text-emerald-600">
                    {formatFieldValue(field, change.new)}
                  </span>
                </div>
              ))}
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

function renderActionType(actionType: string) {
  switch (actionType) {
    case "CREATE_PRODUCT": return "创建商品";
    case "UPDATE_PRODUCT": return "更新商品";
    case "UPDATE_PRODUCT_STATUS": return "上下架";
    case "DELETE_PRODUCT": return "删除商品";
    default: return actionType;
  }
}

function renderFieldName(field: string) {
  const names: Record<string, string> = {
    name: "商品名称", description: "描述", status: "状态",
    salePrice: "售价", categoryId: "分类", images: "图片",
  };
  return names[field] ?? field;
}

function formatFieldValue(field: string, value: string | number | null) {
  if (value === null || value === undefined) return "—";
  if (field === "salePrice") return `¥${Number(value).toFixed(2)}`;
  if (field === "status") {
    const labels: Record<string, string> = {
      DRAFT: "草稿", ON_SHELF: "已上架", OFF_SHELF: "已下架",
    };
    return labels[String(value)] ?? String(value);
  }
  return String(value);
}

function summarizeChanges(
  fieldChanges: Record<string, { old: unknown; new: unknown }> | null
) {
  if (!fieldChanges) return "—";
  const entries = Object.entries(fieldChanges);
  if (entries.length === 0) return "—";
  return (
    entries
      .slice(0, 3)
      .map(
        ([field, change]) =>
          `${renderFieldName(field)}: ${formatFieldValue(field, change.old as string | number | null)}→${formatFieldValue(field, change.new as string | number | null)}`
      )
      .join(", ") + (entries.length > 3 ? ` 等${entries.length}项` : "")
  );
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function buildPageHref(
  filters: AdminProductLogCenterProps["filters"],
  page: number
) {
  const sp = new URLSearchParams();
  if (filters.actionType) sp.set("actionType", filters.actionType);
  if (filters.productName) sp.set("productName", filters.productName);
  if (filters.spuCode) sp.set("spuCode", filters.spuCode);
  if (filters.operatorUserId) sp.set("operatorUserId", filters.operatorUserId);
  sp.set("page", String(page));
  return `/admin/product-logs?${sp.toString()}`;
}
