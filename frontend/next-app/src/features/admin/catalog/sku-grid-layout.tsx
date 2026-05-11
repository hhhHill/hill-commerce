"use client";

import { type ReactNode } from "react";

type SkuGridLayoutProps = {
  header: ReactNode;
  rows: ReactNode;
};

const SKU_GRID_CLASS =
  "grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(180px,1.2fr)_minmax(180px,1fr)_minmax(120px,0.8fr)_minmax(120px,0.8fr)_minmax(150px,0.9fr)_140px]";
const SKU_HEADER_GRID_CLASS =
  "hidden xl:grid xl:gap-3 xl:grid-cols-[minmax(180px,1.2fr)_minmax(180px,1fr)_minmax(120px,0.8fr)_minmax(120px,0.8fr)_minmax(150px,0.9fr)_140px]";

export function SkuGridLayout({ header, rows }: SkuGridLayoutProps) {
  return (
    <div className="space-y-3">
      <div className={`${SKU_HEADER_GRID_CLASS} text-xs font-semibold uppercase tracking-[0.14em] text-black/45`}>
        {header}
      </div>
      <div className="space-y-3">{rows}</div>
    </div>
  );
}

export { SKU_GRID_CLASS };
