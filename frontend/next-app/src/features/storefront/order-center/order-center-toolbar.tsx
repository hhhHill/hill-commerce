import Link from "next/link";

import type { OrderListStatus } from "@/lib/order/types";

type OrderCenterToolbarProps = {
  currentStatus?: OrderListStatus;
  currentOrderNo?: string;
};

const STATUS_OPTIONS: Array<{ label: string; value?: OrderListStatus }> = [
  { label: "全部订单" },
  { label: "待支付", value: "PENDING_PAYMENT" },
  { label: "已支付", value: "PAID" },
  { label: "已取消", value: "CANCELLED" },
  { label: "已关闭", value: "CLOSED" }
];

export function OrderCenterToolbar({ currentStatus, currentOrderNo }: OrderCenterToolbarProps) {
  return (
    <section className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap gap-3">
          {STATUS_OPTIONS.map((option) => {
            const isActive = option.value === currentStatus || (!option.value && !currentStatus);
            return (
              <Link
                key={option.label}
                className={
                  isActive
                    ? "rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white"
                    : "rounded-full border border-black/10 px-4 py-2 text-sm font-medium text-black/70"
                }
                href={`/orders${buildQueryString({ status: option.value, orderNo: currentOrderNo })}`}
              >
                {option.label}
              </Link>
            );
          })}
        </div>

        <form action="/orders" className="flex flex-col gap-3 md:flex-row md:items-center">
          {currentStatus ? <input name="status" type="hidden" value={currentStatus} /> : null}
          <input
            className="w-full rounded-full border border-black/10 bg-[var(--surface)] px-5 py-3 text-sm outline-none transition focus:border-black/20"
            defaultValue={currentOrderNo ?? ""}
            name="orderNo"
            placeholder="输入订单号前缀搜索"
            type="search"
          />
          <button className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white" type="submit">
            搜索订单
          </button>
          <Link className="rounded-full border border-black/10 px-5 py-3 text-sm font-medium" href="/orders">
            清空条件
          </Link>
        </form>

        <p className="text-sm text-black/50">订单号搜索采用前缀匹配，输入不足 4 位时会回到默认列表。</p>
      </div>
    </section>
  );
}

function buildQueryString({ status, orderNo, page }: { status?: OrderListStatus; orderNo?: string; page?: number }) {
  const searchParams = new URLSearchParams();
  if (status) {
    searchParams.set("status", status);
  }
  if (orderNo?.trim()) {
    searchParams.set("orderNo", orderNo.trim());
  }
  if (page && page > 1) {
    searchParams.set("page", String(page));
  }

  const search = searchParams.toString();
  return search ? `?${search}` : "";
}
