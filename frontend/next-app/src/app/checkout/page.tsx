import Link from "next/link";

import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { CheckoutPanel } from "@/features/storefront/order/checkout-panel";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { getServerOrderCheckout } from "@/lib/order/server";

export default async function CheckoutPage() {
  await requireUser("/checkout");
  const checkout = await getServerOrderCheckout();

  return (
    <main className="min-h-screen bg-white pb-28">
      <div className="mx-auto max-w-[800px] px-4">
        {/* top bar */}
        <div className="flex items-center justify-between py-3">
          <div className="flex items-center gap-3">
            <HomeShortcut />
            <Link className="text-sm text-[var(--text-secondary)]" href="/cart">
              返回购物车
            </Link>
          </div>
          <SearchForm />
        </div>

        {/* title */}
        <h1 className="py-2 text-lg font-semibold">确认订单</h1>

        <CheckoutPanel checkout={checkout} />
      </div>
    </main>
  );
}
