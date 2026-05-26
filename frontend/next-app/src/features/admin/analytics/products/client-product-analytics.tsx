"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";

import { getProductRankings } from "@/lib/admin/analytics-client";
import type { ProductRankItem } from "@/lib/admin/analytics-types";
import type { ProductViewLogEntry, ProductViewLogListResult } from "@/lib/admin/types";
import { CategoryPieChart } from "./category-pie-chart";
import { ProductRankingTable } from "./product-ranking-table";

const RANGE_TABS = [
  { key: "today", label: "今日" },
  { key: "week", label: "本周" },
  { key: "month", label: "本月" }
] as const;

type Props = {
  viewLogs?: ProductViewLogListResult;
  viewLogFilters?: {
    productId?: string;
    categoryId?: string;
  };
};

export function ClientProductAnalytics({ viewLogs, viewLogFilters }: Props) {
  const searchParams = useSearchParams();
  const tab = searchParams.get("tab");
  const isViewLogsTab = tab === "view-logs";

  const [range, setRange] = useState("today");
  const [items, setItems] = useState<ProductRankItem[]>([]);

  useEffect(() => {
    if (isViewLogsTab) return;

    let active = true;
    getProductRankings({ range, limit: 50 })
      .then((response) => {
        if (active) setItems(response.items);
      })
      .catch(() => {
        if (active) setItems([]);
      });
    return () => {
      active = false;
    };
  }, [range, isViewLogsTab]);

  return (
    <div className="space-y-6">
      <nav className="flex gap-2">
        {RANGE_TABS.map((t) => (
          <button
            key={t.key}
            className={`rounded-full px-4 py-1.5 text-sm font-medium ${
              !isViewLogsTab && range === t.key
                ? "bg-[var(--brand-primary)] text-white"
                : "border border-[var(--border-normal)] bg-white"
            }`}
            onClick={() => setRange(t.key)}
            type="button"
          >
            {t.label}
          </button>
        ))}
        <a
          href="/admin/analytics/products?tab=view-logs"
          className={`rounded-full px-4 py-1.5 text-sm font-medium ${
            isViewLogsTab
              ? "bg-[var(--brand-primary)] text-white"
              : "border border-[var(--border-normal)] bg-white"
          }`}
        >
          浏览记录
        </a>
      </nav>

      {isViewLogsTab ? (
        <ViewLogTable logs={viewLogs?.items ?? []} filters={viewLogFilters} />
      ) : (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <ProductRankingTable items={items} />
          </div>
          <CategoryPieChart items={items} />
        </div>
      )}
    </div>
  );
}

function ViewLogTable({
  logs,
  filters
}: {
  logs: ProductViewLogEntry[];
  filters?: { productId?: string; categoryId?: string };
}) {
  return (
    <div>
      <form action="/admin/analytics/products" className="mb-4 flex flex-wrap gap-3">
        <input name="tab" type="hidden" value="view-logs" />
        <input
          className="rounded-lg border border-[var(--border-normal)] bg-white px-4 py-2 text-sm outline-none transition focus:border-[var(--brand-primary)]"
          defaultValue={filters?.productId ?? ""}
          name="productId"
          placeholder="商品 ID"
        />
        <input
          className="rounded-lg border border-[var(--border-normal)] bg-white px-4 py-2 text-sm outline-none transition focus:border-[var(--brand-primary)]"
          defaultValue={filters?.categoryId ?? ""}
          name="categoryId"
          placeholder="分类 ID"
        />
        <button
          className="rounded-lg bg-[var(--brand-primary)] px-5 py-2 text-sm font-semibold text-white"
          type="submit"
        >
          查询
        </button>
      </form>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#f0f0f0] text-left text-xs text-[var(--text-hint)]">
              <th className="px-4 py-2.5 font-medium">商品ID</th>
              <th className="px-4 py-2.5 font-medium">分类ID</th>
              <th className="px-4 py-2.5 font-medium">用户</th>
              <th className="px-4 py-2.5 font-medium">匿名标识</th>
              <th className="px-4 py-2.5 font-medium">浏览时间</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((item) => (
              <tr
                key={item.id}
                className="border-b border-[#f5f5f5] transition-colors hover:bg-[#fafafa]"
              >
                <td className="px-4 py-2.5 font-medium text-[var(--text-primary)]">
                  {item.productId}
                </td>
                <td className="px-4 py-2.5 text-[var(--text-secondary)]">
                  {item.categoryId}
                </td>
                <td className="px-4 py-2.5 text-[var(--text-secondary)]">
                  {item.userId ? `用户 ${item.userId}` : "匿名浏览"}
                </td>
                <td className="px-4 py-2.5 text-[var(--text-hint)]">
                  {item.anonymousId ?? "登录用户"}
                </td>
                <td className="whitespace-nowrap px-4 py-2.5 text-[var(--text-hint)]">
                  {formatDateTime(item.viewedAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {logs.length === 0 ? (
        <p className="px-4 py-10 text-center text-sm text-[var(--text-hint)]">
          当前筛选下没有浏览日志。
        </p>
      ) : null}
    </div>
  );
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
