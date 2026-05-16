"use client";

import type { ProductRankItem } from "@/lib/admin/analytics-types";

type Props = {
  items: ProductRankItem[];
};

export function ProductRankingTable({ items }: Props) {
  return (
    <div className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <h3 className="mb-4 text-lg font-semibold">商品销量排行</h3>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-black/10 text-left">
              <th className="pb-3 font-medium text-black/55">#</th>
              <th className="pb-3 font-medium text-black/55">商品</th>
              <th className="pb-3 font-medium text-black/55">品类</th>
              <th className="pb-3 text-right font-medium text-black/55">销量</th>
              <th className="pb-3 text-right font-medium text-black/55">销售额</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <tr key={`${item.productId}-${index}`} className="border-b border-black/5">
                <td className="py-3 text-black/40">{index + 1}</td>
                <td className="py-3 font-medium">{item.productName}</td>
                <td className="py-3 text-black/55">{item.categoryName ?? "未知"}</td>
                <td className="py-3 text-right">{item.totalQuantity}</td>
                <td className="py-3 text-right font-semibold text-[var(--accent-strong)]">{formatCurrency(item.totalAmount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {items.length === 0 ? <p className="py-8 text-center text-sm text-black/55">暂无排行数据。</p> : null}
    </div>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(value);
}
