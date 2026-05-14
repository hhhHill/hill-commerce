import Link from "next/link";

import type { OrderListStatus } from "@/lib/order/types";

type OrderCenterPaginationProps = {
  page: number;
  totalPages: number;
  status?: OrderListStatus;
  orderNo?: string;
};

export function OrderCenterPagination({ page, totalPages, status, orderNo }: OrderCenterPaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-[24px] border border-black/10 bg-white/70 px-5 py-4">
      <p className="text-sm text-black/55">
        第 {page} / {totalPages} 页
      </p>
      <div className="flex gap-3">
        {page > 1 ? (
          <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href={`/orders${buildQueryString(status, orderNo, page - 1)}`}>
            上一页
          </Link>
        ) : null}
        {page < totalPages ? (
          <Link className="rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white" href={`/orders${buildQueryString(status, orderNo, page + 1)}`}>
            下一页
          </Link>
        ) : null}
      </div>
    </div>
  );
}

function buildQueryString(status: OrderListStatus | undefined, orderNo: string | undefined, page: number) {
  const searchParams = new URLSearchParams();
  searchParams.set("page", String(page));
  if (status) {
    searchParams.set("status", status);
  }
  if (orderNo?.trim()) {
    searchParams.set("orderNo", orderNo.trim());
  }
  return `?${searchParams.toString()}`;
}
