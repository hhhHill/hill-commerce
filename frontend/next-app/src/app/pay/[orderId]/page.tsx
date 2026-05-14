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
      <main className="min-h-screen px-6 py-10">
        <div className="mx-auto flex max-w-6xl flex-col gap-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap gap-3">
              <HomeShortcut />
              <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href={`/orders/${orderId}/result`}>
                返回订单结果页
              </Link>
            </div>
            <SearchForm className="w-full max-w-md" />
          </div>

          <section className="space-y-3">
            <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
              Pay
            </span>
            <h1 className="text-4xl font-semibold tracking-tight">订单支付</h1>
            <p className="max-w-3xl text-sm leading-7 text-black/65">这里承接 `order-checkout` 已经创建完成的订单，只处理支付尝试、模拟支付成功 / 失败，以及订单终态展示。</p>
          </section>

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
