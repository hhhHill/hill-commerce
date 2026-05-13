import Link from "next/link";

import type { OrderCheckoutAddress } from "@/lib/order/types";

type CheckoutAddressCardProps = {
  address: OrderCheckoutAddress | null;
};

export function CheckoutAddressCard({ address }: CheckoutAddressCardProps) {
  if (!address) {
    return (
      <article className="rounded-[28px] border border-dashed border-red-200 bg-red-50/80 p-5 text-red-800">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm uppercase tracking-[0.18em] text-red-700/75">Default Address</p>
            <h2 className="mt-2 text-2xl font-semibold">缺少默认地址</h2>
          </div>
          <Link className="rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white" href="/account/addresses">
            去新增地址
          </Link>
        </div>
        <p className="mt-3 text-sm leading-7">当前无法提交订单。先补充或修正收货地址，系统会自动维护唯一默认地址。</p>
      </article>
    );
  }

  return (
    <article className="rounded-[28px] border border-black/10 bg-white/85 p-5 shadow-[0_18px_50px_rgba(74,42,18,0.06)]">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm uppercase tracking-[0.18em] text-black/45">Default Address</p>
          <h2 className="mt-2 text-2xl font-semibold">{address.receiverName}</h2>
          <p className="mt-1 text-sm text-black/60">{address.receiverPhone}</p>
        </div>
        <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/account/addresses">
          管理地址
        </Link>
      </div>
      <p className="mt-4 text-sm leading-7 text-black/65">
        {address.province}
        {address.city}
        {address.district}
        {address.detailAddress}
        {address.postalCode ? ` · ${address.postalCode}` : ""}
      </p>
    </article>
  );
}
