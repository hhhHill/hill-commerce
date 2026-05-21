import { notFound } from "next/navigation";

import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { OrderResultPanel } from "@/features/storefront/order/order-result-panel";
import { requireUser } from "@/lib/auth/server";
import { OrderRequestError } from "@/lib/order/errors";
import { getServerOrder } from "@/lib/order/server";

type OrderResultPageProps = {
  params: Promise<{
    orderId: string;
  }>;
};

export default async function OrderResultPage({ params }: OrderResultPageProps) {
  const { orderId } = await params;
  await requireUser(`/orders/${orderId}/result`);

  try {
    const order = await getServerOrder(Number(orderId));

    return (
      <main className="min-h-screen bg-white pb-28">
        <div className="mx-auto flex max-w-[800px] flex-col">
          <div className="px-4 pt-4">
            <HomeShortcut />
          </div>

          <OrderResultPanel order={order} />
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
