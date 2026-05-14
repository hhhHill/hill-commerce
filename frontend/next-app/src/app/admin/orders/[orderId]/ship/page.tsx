import Link from "next/link";
import { notFound } from "next/navigation";

import { AdminShipmentForm } from "@/features/admin/order/admin-shipment-form";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { requireRole } from "@/lib/auth/server";
import { getAdminShipOrder } from "@/lib/admin/server";
import { OrderRequestError } from "@/lib/order/errors";

type AdminShipOrderPageProps = {
  params: Promise<{
    orderId: string;
  }>;
};

export default async function AdminShipOrderPage({ params }: AdminShipOrderPageProps) {
  const { orderId } = await params;
  const user = await requireRole(["ADMIN", "SALES"], `/admin/orders/${orderId}/ship`);

  try {
    const order = await getAdminShipOrder(Number(orderId));

    return (
      <AdminShell
        description="录入快递公司和运单号后，订单会从已支付推进到已发货。"
        title="订单发货"
        user={user}
      >
        {order.orderStatus === "PAID" ? (
          <AdminShipmentForm order={order} />
        ) : (
          <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
            <p className="text-lg font-semibold">当前订单状态为 {order.orderStatus}，不可发货。</p>
            <Link className="mt-4 inline-flex rounded-full border border-black/10 px-5 py-3 text-sm font-medium" href="/admin/orders">
              返回订单列表
            </Link>
          </section>
        )}
      </AdminShell>
    );
  } catch (error) {
    if (error instanceof OrderRequestError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}
