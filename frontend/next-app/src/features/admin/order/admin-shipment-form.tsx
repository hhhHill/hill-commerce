"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState, useTransition } from "react";

import { shipOrder } from "@/lib/admin/client";
import type { OrderDetail } from "@/lib/order/types";

type AdminShipmentFormProps = {
  order: OrderDetail;
};

export function AdminShipmentForm({ order }: AdminShipmentFormProps) {
  const router = useRouter();
  const [carrierName, setCarrierName] = useState("");
  const [trackingNo, setTrackingNo] = useState("");
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    startTransition(async () => {
      try {
        await shipOrder(order.id, carrierName, trackingNo);
        router.push("/admin/orders");
        router.refresh();
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "发货失败");
      }
    });
  }

  return (
    <div className="space-y-6">
      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h2 className="text-2xl font-semibold">订单信息</h2>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          <InfoCard label="订单号" value={order.orderNo} />
          <InfoCard label="订单状态" value={renderStatus(order.orderStatus)} />
          <InfoCard label="订单金额" value={`¥${order.payableAmount.toFixed(2)}`} />
          <InfoCard label="商品摘要" value={renderSummary(order)} />
        </div>
        <div className="mt-4 rounded-[24px] bg-[#fffaf5] p-5 text-sm leading-7 text-black/65">
          <p className="font-semibold text-black/80">收货地址</p>
          <p className="mt-2">{order.address.receiverName} / {order.address.receiverPhone}</p>
          <p>
            {order.address.province}
            {order.address.city}
            {order.address.district}
            {order.address.detailAddress}
            {order.address.postalCode ? ` · ${order.address.postalCode}` : ""}
          </p>
        </div>
      </section>

      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h2 className="text-2xl font-semibold">录入物流</h2>
        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          <label className="flex flex-col gap-2 text-sm font-medium">
            快递公司
            <input
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              onChange={(event) => setCarrierName(event.target.value)}
              placeholder="例如 SF Express"
              value={carrierName}
            />
          </label>
          <label className="flex flex-col gap-2 text-sm font-medium">
            运单号
            <input
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              onChange={(event) => setTrackingNo(event.target.value)}
              placeholder="请输入运单号"
              value={trackingNo}
            />
          </label>
          <div className="flex flex-wrap gap-3">
            <button className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white" disabled={isPending} type="submit">
              {isPending ? "提交中..." : "确认发货"}
            </button>
            <Link className="rounded-full border border-black/10 px-5 py-3 text-sm font-medium" href="/admin/orders">
              返回订单列表
            </Link>
          </div>
          {message ? <p className="text-sm text-red-700">{message}</p> : null}
        </form>
      </section>
    </div>
  );
}

function InfoCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[24px] bg-[#fffaf5] p-5">
      <p className="text-sm text-black/50">{label}</p>
      <p className="mt-2 text-lg font-semibold">{value}</p>
    </div>
  );
}

function renderStatus(status: string) {
  switch (status) {
    case "PAID":
      return "已支付";
    case "SHIPPED":
      return "已发货";
    case "COMPLETED":
      return "已完成";
    case "PENDING_PAYMENT":
      return "待支付";
    case "CANCELLED":
      return "已取消";
    case "CLOSED":
      return "已关闭";
    default:
      return status;
  }
}

function renderSummary(order: OrderDetail) {
  const firstItem = order.items[0];
  if (!firstItem) {
    return "暂无商品";
  }
  if (order.items.length === 1) {
    return firstItem.productName;
  }
  return `${firstItem.productName} 等 ${order.items.length} 件`;
}
