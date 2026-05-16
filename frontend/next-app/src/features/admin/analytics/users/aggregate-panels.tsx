"use client";

import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import type { AggregateProfileResponse } from "@/lib/admin/analytics-types";

type AggregatePanelsProps = {
  data: AggregateProfileResponse;
};

const COLORS = ["#ff4d1e", "#f59e0b", "#111827", "#0ea5e9", "#10b981", "#f43f5e"];
const TIER_LABELS: Record<string, string> = { low: "低消费", mid: "中消费", high: "高消费" };

export function AggregatePanels({ data }: AggregatePanelsProps) {
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <article className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h3 className="mb-4 text-lg font-semibold">地域分布</h3>
        <ResponsiveContainer height={280} width="100%">
          <BarChart data={data.regionDistribution}>
            <CartesianGrid stroke="#00000015" strokeDasharray="3 3" />
            <XAxis dataKey="region" tick={{ fontSize: 11 }} />
            <YAxis tick={{ fontSize: 12 }} />
            <Tooltip />
            <Bar dataKey="userCount" fill="#ff4d1e" radius={[6, 6, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </article>
      <article className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h3 className="mb-4 text-lg font-semibold">购买力分层</h3>
        <ResponsiveContainer height={280} width="100%">
          <PieChart>
            <Pie data={data.purchasingPowerTiers} dataKey="userCount" nameKey="tier" outerRadius={96} label>
              {data.purchasingPowerTiers.map((entry, index) => (
                <Cell key={entry.tier} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
      </article>
      <article className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)] lg:col-span-2">
        <h3 className="mb-4 text-lg font-semibold">偏好品类</h3>
        <div className="space-y-3">
          {data.categoryPreferences.map((category, index) => (
            <div key={category.categoryId} className="flex items-center justify-between rounded-[18px] bg-[#fffaf5] px-4 py-3">
              <span className="text-sm">#{index + 1} {category.categoryName}</span>
              <span className="font-semibold text-[var(--accent-strong)]">{category.orderCount} 单</span>
            </div>
          ))}
        </div>
      </article>
      <div className="grid gap-4 sm:grid-cols-3 lg:col-span-2">
        <Metric label="总用户数" value={data.totalUsers} />
        <Metric label="复购用户数" value={data.repeatPurchaseUsers} />
        <Metric label="复购率" value={`${data.repeatPurchaseRate}%`} />
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-[20px] bg-[#fffaf5] p-4 text-center">
      <p className="text-sm text-black/55">{label}</p>
      <p className="mt-2 text-2xl font-bold">{value}</p>
    </div>
  );
}
