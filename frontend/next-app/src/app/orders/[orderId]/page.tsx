import Link from "next/link";
import { notFound } from "next/navigation";

import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { OrderDetailPanel } from "@/features/storefront/order/order-detail-panel";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { OrderRequestError } from "@/lib/order/errors";
import { getServerOrder } from "@/lib/order/server";

type OrderDetailPageProps = {
  params: Promise<{
    orderId: string;
  }>;
};

export default async function OrderDetailPage({ params }: OrderDetailPageProps) {
  const { orderId } = await params;
  await requireUser(`/orders/${orderId}`);

  try {
    const order = await getServerOrder(Number(orderId));

    return (
      <main className="page-shell">
        <div className="page-stack">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap gap-3">
              <HomeShortcut />
              <Link className="btn-secondary px-4 py-2" href={`/orders/${orderId}/result`}>
                返回提交结果页
              </Link>
            </div>
            <SearchForm />
          </div>

          <OrderDetailPanel order={order} />
        </div>
      </main>
    );
  } catch (error) {
    if (error instanceof OrderRequestError && error.status === 404) {
      notFound();
    }

    throw error;
  }
}
