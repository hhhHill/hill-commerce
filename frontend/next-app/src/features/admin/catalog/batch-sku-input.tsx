"use client";

import { useState } from "react";

type BatchSkuInputProps = {
  onApply: (fields: { price?: string; stock?: string; threshold?: string }) => void;
};

export function BatchSkuInput({ onApply }: BatchSkuInputProps) {
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [threshold, setThreshold] = useState("");

  function handleApply() {
    if (!price && !stock && !threshold) return;
    onApply({
      price: price || undefined,
      stock: stock || undefined,
      threshold: threshold || undefined,
    });
  }

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-lg border border-[#f0f0f0] bg-[#fafafa] px-3 py-2 text-sm">
      <span className="text-xs font-medium text-[var(--text-secondary)]">批量设置：</span>
      <label className="flex items-center gap-1 text-xs text-[var(--text-hint)]">
        价格
        <input
          className="w-20 rounded border border-[#e0e0e0] px-1.5 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
          min="0"
          placeholder="0.00"
          step="0.01"
          type="number"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
        />
      </label>
      <label className="flex items-center gap-1 text-xs text-[var(--text-hint)]">
        库存
        <input
          className="w-20 rounded border border-[#e0e0e0] px-1.5 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
          min="0"
          placeholder="0"
          type="number"
          value={stock}
          onChange={(e) => setStock(e.target.value)}
        />
      </label>
      <label className="flex items-center gap-1 text-xs text-[var(--text-hint)]">
        阈值
        <input
          className="w-20 rounded border border-[#e0e0e0] px-1.5 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
          min="0"
          placeholder="0"
          type="number"
          value={threshold}
          onChange={(e) => setThreshold(e.target.value)}
        />
      </label>
      <button
        className="rounded bg-[var(--brand-primary)] px-3 py-1 text-xs font-medium text-white hover:opacity-90"
        onClick={handleApply}
        type="button"
      >
        应用到全部 SKU
      </button>
      <span className="text-xs text-[var(--text-hint)]">已有数据会被覆盖</span>
    </div>
  );
}
