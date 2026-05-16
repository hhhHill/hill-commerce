import type { ProductRankItem } from "@/lib/admin/analytics-types";

type MiniProductRankingProps = {
  items: ProductRankItem[];
};

export function MiniProductRanking({ items }: MiniProductRankingProps) {
  return (
    <article className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-semibold">商品销量 Top 10</h2>
        <a className="text-sm font-semibold text-[var(--brand-primary)]" href="/admin/analytics/products">
          查看完整分析
        </a>
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        {items.slice(0, 10).map((item, index) => (
          <div key={`${item.productId}-${index}`} className="flex items-center justify-between rounded-[18px] bg-[#fffaf5] px-4 py-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold">#{index + 1} {item.productName}</p>
              <p className="mt-1 text-xs text-black/50">{item.categoryName || "未知品类"}</p>
            </div>
            <div className="ml-4 text-right">
              <p className="text-sm font-bold text-[var(--accent-strong)]">{item.totalQuantity}</p>
              <p className="text-xs text-black/45">件</p>
            </div>
          </div>
        ))}
      </div>
      {items.length === 0 ? <p className="rounded-[18px] border border-dashed border-black/10 py-8 text-center text-sm text-black/55">暂无商品排行数据。</p> : null}
    </article>
  );
}
