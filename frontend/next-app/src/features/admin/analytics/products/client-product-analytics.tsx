"use client";

import { useEffect, useState } from "react";

import { getProductRankings } from "@/lib/admin/analytics-client";
import type { ProductRankItem } from "@/lib/admin/analytics-types";
import { CategoryPieChart } from "./category-pie-chart";
import { ProductRankingTable } from "./product-ranking-table";

const RANGE_TABS = [
  { key: "today", label: "今日" },
  { key: "week", label: "本周" },
  { key: "month", label: "本月" }
] as const;

export function ClientProductAnalytics() {
  const [range, setRange] = useState("today");
  const [items, setItems] = useState<ProductRankItem[]>([]);

  useEffect(() => {
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
  }, [range]);

  return (
    <div className="space-y-6">
      <nav className="flex gap-2">
        {RANGE_TABS.map((tab) => (
          <button
            key={tab.key}
            className={`rounded-full px-4 py-1.5 text-sm font-medium ${
              range === tab.key ? "bg-[var(--brand-primary)] text-white" : "border border-[var(--border-normal)] bg-white"
            }`}
            onClick={() => setRange(tab.key)}
            type="button"
          >
            {tab.label}
          </button>
        ))}
      </nav>
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <ProductRankingTable items={items} />
        </div>
        <CategoryPieChart items={items} />
      </div>
    </div>
  );
}
