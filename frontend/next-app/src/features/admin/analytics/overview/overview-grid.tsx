import type { ProductRankItem, TrendResponse } from "@/lib/admin/analytics-types";
import { AnomalyBanner } from "./anomaly-banner";
import { KpiCards } from "./kpi-cards";
import { MiniProductRanking } from "./mini-product-ranking";
import { SalesTrendChart } from "./sales-trend-chart";

type OverviewGridProps = {
  trends: TrendResponse | null;
  topProducts: ProductRankItem[];
};

export function OverviewGrid({ trends, topProducts }: OverviewGridProps) {
  return (
    <div className="space-y-6">
      <AnomalyBanner />
      <KpiCards trends={trends} />
      <SalesTrendChart initial={trends} />
      <MiniProductRanking items={topProducts} />
    </div>
  );
}
