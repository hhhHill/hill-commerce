import type { DashboardSummary } from "@/lib/admin/types";

type AdminDashboardProps = {
  summary: DashboardSummary;
};

const STATUS_LABELS: Array<{ key: string; label: string }> = [
  { key: "PENDING_PAYMENT", label: "待支付" },
  { key: "PAID", label: "已支付" },
  { key: "SHIPPED", label: "已发货" },
  { key: "COMPLETED", label: "已完成" },
  { key: "CANCELLED", label: "已取消" },
  { key: "CLOSED", label: "已关闭" }
];

export function AdminDashboard({ summary }: AdminDashboardProps) {
  return (
    <section className="grid gap-6 lg:grid-cols-3">
      <article className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h2 className="text-2xl font-semibold">订单状态分布</h2>
        <div className="mt-5 grid gap-3 sm:grid-cols-2">
          {STATUS_LABELS.map((status) => (
            <div key={status.key} className="rounded-[20px] bg-[#fffaf5] p-4">
              <p className="text-sm text-black/55">{status.label}</p>
              <p className="mt-2 text-2xl font-semibold">{summary.orderStatusCounts[status.key] ?? 0}</p>
            </div>
          ))}
        </div>
      </article>

      <article className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h2 className="text-2xl font-semibold">总销售额</h2>
        <p className="mt-5 text-4xl font-bold text-[var(--accent-strong)]">{formatCurrency(summary.totalSalesAmount)}</p>
        <p className="mt-3 text-sm leading-6 text-black/65">统计范围包含 `PAID`、`SHIPPED`、`COMPLETED` 订单的 payable amount 合计。</p>
      </article>

      <article className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h2 className="text-2xl font-semibold">Sales 发货排行</h2>
        <div className="mt-5 space-y-3">
          {summary.salesRanking.map((item, index) => (
            <div key={`${item.nickname}-${index}`} className="flex items-center justify-between rounded-[20px] bg-[#fffaf5] px-4 py-3">
              <div>
                <p className="text-sm text-black/50">#{index + 1}</p>
                <p className="font-semibold">{item.nickname}</p>
              </div>
              <p className="text-lg font-semibold text-[var(--accent-strong)]">{item.orderCount}</p>
            </div>
          ))}
          {summary.salesRanking.length === 0 ? (
            <p className="rounded-[20px] border border-dashed border-black/10 px-4 py-6 text-center text-sm text-black/55">
              暂无发货记录。
            </p>
          ) : null}
        </div>
      </article>
    </section>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY"
  }).format(value);
}
