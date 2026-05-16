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
  { label: "已发货", value: "SHIPPED" },
  { label: "已完成", value: "COMPLETED" },
  { label: "已取消", value: "CANCELLED" },
  { label: "已关闭", value: "CLOSED" }
];

export function OrderCenterToolbar({ currentStatus, currentOrderNo }: OrderCenterToolbarProps) {
  return (
    <section className="surface-card rounded-lg p-4">
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap gap-3">
          {STATUS_OPTIONS.map((option) => {
            const isActive = option.value === currentStatus || (!option.value && !currentStatus);
            return (
              <Link
                key={option.label}
                className={
                  isActive
                    ? "btn-primary px-4 py-2"
                    : "btn-secondary px-4 py-2"
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
            className="w-full rounded-full border border-[var(--border-normal)] bg-white px-5 py-3 text-sm outline-none transition focus:border-[var(--brand-primary)]"
            defaultValue={currentOrderNo ?? ""}
            name="orderNo"
            placeholder="输入订单号前缀搜索"
            type="search"
          />
          <button className="btn-primary px-5 py-3" type="submit">
            搜索订单
          </button>
          <Link className="btn-secondary px-5 py-3" href="/orders">
            清空条件
          </Link>
        </form>

        <p className="text-sm text-[var(--text-secondary)]">订单号搜索采用前缀匹配，输入不足 4 位时会回到默认列表。</p>
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
