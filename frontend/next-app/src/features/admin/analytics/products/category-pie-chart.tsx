"use client";

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

import type { ProductRankItem } from "@/lib/admin/analytics-types";

type Props = {
  items: ProductRankItem[];
};

const COLORS = ["#ff4d1e", "#f59e0b", "#111827", "#0ea5e9", "#10b981", "#f43f5e", "#8b5cf6", "#14b8a6"];

export function CategoryPieChart({ items }: Props) {
  const categoryMap = new Map<string, number>();
  for (const item of items) {
    const name = item.categoryName || "未知";
    categoryMap.set(name, (categoryMap.get(name) ?? 0) + item.totalAmount);
  }
  const data = Array.from(categoryMap.entries()).map(([name, value]) => ({ name, value }));

  return (
    <div className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <h3 className="mb-4 text-lg font-semibold">品类占比</h3>
      <ResponsiveContainer height={280} width="100%">
        <PieChart>
          <Pie data={data} dataKey="value" nameKey="name" outerRadius={96} label>
            {data.map((entry, index) => (
              <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
