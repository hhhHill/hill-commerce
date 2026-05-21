import Link from "next/link";
import { notFound } from "next/navigation";

import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { PaymentPanel } from "@/features/storefront/payment/payment-panel";
import { requireUser } from "@/lib/auth/server";
import { PaymentRequestError } from "@/lib/payment/errors";
import { getServerPaymentOrder } from "@/lib/payment/server";

type PaymentPageProps = {
  params: Promise<{
    orderId: string;
  }>;
};

export default async function PaymentPage({ params }: PaymentPageProps) {
  const { orderId } = await params;
  await requireUser(`/pay/${orderId}`);

  try {
    const payment = await getServerPaymentOrder(Number(orderId));

    return (
      <main className="min-h-screen bg-white pb-28">
        <div className="mx-auto max-w-[800px] px-4">
          <div className="flex items-center justify-between py-3">
            <div className="flex items-center gap-3">
              <HomeShortcut />
              <Link
                className="text-sm text-[var(--text-secondary)]"
                href={`/orders/${orderId}/result`}
              >
                返回订单结果
              </Link>
            </div>
            <SearchForm />
          </div>

          <h1 className="py-2 text-lg font-semibold">订单支付</h1>

          <PaymentPanel payment={payment} />
        </div>
      </main>
    );
  } catch (error) {
    if (error instanceof PaymentRequestError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}
