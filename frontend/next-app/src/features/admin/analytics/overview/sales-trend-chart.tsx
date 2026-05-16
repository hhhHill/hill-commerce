"use client";

import { useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import { getAnalyticsTrends } from "@/lib/admin/analytics-client";
import type { TrendPoint, TrendResponse } from "@/lib/admin/analytics-types";

type SalesTrendChartProps = {
  initial: TrendResponse | null;
};

const GRANULARITIES = [
  { key: "day", label: "日" },
  { key: "week", label: "周" },
  { key: "month", label: "月" }
] as const;

export function SalesTrendChart({ initial }: SalesTrendChartProps) {
  const [granularity, setGranularity] = useState(initial?.granularity ?? "day");
  const [points, setPoints] = useState<TrendPoint[]>(initial?.points ?? []);

  const switchGranularity = async (next: string) => {
    setGranularity(next);
    try {
      const data = await getAnalyticsTrends({ granularity: next });
      setPoints(data.points);
    } catch {
      setPoints([]);
    }
  };

  return (
    <article className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <div className="mb-4 flex items-center justify-between gap-4">
        <h2 className="text-xl font-semibold">销售趋势</h2>
        <div className="flex gap-2">
          {GRANULARITIES.map((item) => (
            <button
              key={item.key}
              className={`rounded-full px-3 py-1 text-sm font-medium ${
                granularity === item.key ? "bg-[var(--brand-primary)] text-white" : "border border-[var(--border-normal)] bg-white"
              }`}
              onClick={() => void switchGranularity(item.key)}
              type="button"
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>
      <ResponsiveContainer height={320} width="100%">
        <LineChart data={points}>
          <CartesianGrid stroke="#00000015" strokeDasharray="3 3" />
          <XAxis dataKey="date" tick={{ fontSize: 12 }} />
          <YAxis tick={{ fontSize: 12 }} />
          <Tooltip />
          <Line dataKey="amount" name="销售额" stroke="#ff4d1e" strokeWidth={3} type="monotone" />
          <Line dataKey="movingAvg" name="7期移动平均" stroke="#111827" strokeDasharray="4 4" strokeWidth={2} type="monotone" />
        </LineChart>
      </ResponsiveContainer>
    </article>
  );
}
