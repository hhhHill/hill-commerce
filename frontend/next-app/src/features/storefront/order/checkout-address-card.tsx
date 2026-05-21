import Link from "next/link";

import type { OrderCheckoutAddress } from "@/lib/order/types";

type CheckoutAddressCardProps = {
  address: OrderCheckoutAddress | null;
};

export function CheckoutAddressCard({ address }: CheckoutAddressCardProps) {
  if (!address) {
    return (
      <Link
        className="mx-4 flex items-center gap-3 border-b border-[#f0f0f0] py-4"
        href="/account/addresses"
      >
        <span className="text-sm font-medium">📍 收货地址</span>
        <span className="text-sm text-red-500">请添加收货地址</span>
        <span className="ml-auto text-[var(--text-hint)]">›</span>
      </Link>
    );
  }

  return (
    <Link
      className="mx-4 flex items-start gap-3 border-b border-[#f0f0f0] py-4"
      href="/account/addresses"
    >
      <span className="mt-0.5 shrink-0 text-sm">📍</span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-3 text-sm">
          <span className="font-semibold">{address.receiverName}</span>
          <span className="text-[var(--text-secondary)]">
            {address.receiverPhone}
          </span>
        </div>
        <p className="mt-0.5 truncate text-sm text-[var(--text-secondary)]">
          {address.province}
          {address.city}
          {address.district}
          {address.detailAddress}
        </p>
      </div>
      <span className="mt-0.5 shrink-0 text-[var(--text-hint)]">›</span>
    </Link>
  );
}
