"use client";

import { useEffect, useState } from "react";

import { getAnomalies, getAnomalyStatus } from "@/lib/admin/analytics-client";
import type { AnomalyItem, AnomalyStatusResponse } from "@/lib/admin/analytics-types";

export function AnomalyBanner() {
  const [status, setStatus] = useState<AnomalyStatusResponse>({ hasAlert: false, count: 0 });
  const [expanded, setExpanded] = useState(false);
  const [items, setItems] = useState<AnomalyItem[]>([]);

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const next = await getAnomalyStatus();
        if (active) setStatus(next);
      } catch {
        if (active) setStatus({ hasAlert: false, count: 0 });
      }
    };
    void load();
    const timer = window.setInterval(load, 30_000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  if (!status.hasAlert) return null;

  const loadItems = async () => {
    setExpanded((value) => !value);
    if (items.length > 0) return;
    try {
      setItems(await getAnomalies());
    } catch {
      setItems([]);
    }
  };

  return (
    <div className="rounded-[24px] border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-700">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="font-semibold">检测到 {status.count} 条销售异常告警。</p>
        <button className="rounded-full bg-red-600 px-4 py-1.5 text-xs font-semibold text-white" onClick={() => void loadItems()} type="button">
          {expanded ? "收起详情" : "查看详情"}
        </button>
      </div>
      {expanded ? (
        <div className="mt-4 space-y-2">
          {items.map((item) => (
            <div key={item.id} className="rounded-2xl bg-white/80 px-4 py-3">
              <p className="font-semibold">
                {item.direction === "high" ? "异常偏高" : "异常偏低"}：{formatCurrency(item.currentAmount)}
              </p>
              <p className="mt-1 text-xs text-red-700/75">
                {item.snapshotHour}，基线均值 {formatCurrency(item.baselineMean)}，偏离 {item.deviationPercent}%
              </p>
            </div>
          ))}
          {items.length === 0 ? <p className="text-xs text-red-700/75">暂无可展示的异常详情。</p> : null}
        </div>
      ) : null}
    </div>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(value);
}
