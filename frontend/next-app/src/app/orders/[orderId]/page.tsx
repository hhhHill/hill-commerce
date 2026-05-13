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
      <main className="min-h-screen px-6 py-10">
        <div className="mx-auto flex max-w-6xl flex-col gap-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap gap-3">
              <HomeShortcut />
              <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href={`/orders/${orderId}/result`}>
                返回提交结果页
              </Link>
            </div>
            <SearchForm className="w-full max-w-md" />
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
