import type { TrendResponse } from "@/lib/admin/analytics-types";

type KpiCardsProps = {
  trends: TrendResponse | null;
};

export function KpiCards({ trends }: KpiCardsProps) {
  const latest = trends?.points.at(-1);
  const previous = trends?.points.at(-2);
  const amount = latest?.amount ?? 0;
  const cards = [
    { label: "当前销售额", value: formatCurrency(amount) },
    { label: "上一周期销售额", value: formatCurrency(previous?.amount ?? 0) },
    { label: "趋势方向", value: directionLabel(trends?.trendDirection) },
    { label: "环比变化", value: `${trends?.changePercent ?? 0}%` }
  ];

  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      {cards.map((card) => (
        <article key={card.label} className="rounded-[24px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
          <p className="text-sm text-black/55">{card.label}</p>
          <p className="mt-2 text-2xl font-bold text-[var(--accent-strong)]">{card.value}</p>
        </article>
      ))}
    </div>
  );
}

function directionLabel(direction?: string) {
  if (direction === "up") return "上升";
  if (direction === "down") return "下降";
  return "稳定";
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(value);
}
