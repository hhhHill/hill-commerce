import Link from "next/link";

import { CartList } from "@/features/storefront/cart/cart-list";
import { CartBottomBar } from "@/features/storefront/cart/cart-bottom-bar";
import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { getServerCart } from "@/lib/cart/server";

export default async function CartPage() {
  await requireUser("/cart");
  const cart = await getServerCart();

  return (
    <main className="min-h-screen bg-white pb-28">
      <div className="mx-auto max-w-[800px] px-4">
        {/* top bar */}
        <div className="flex items-center justify-between py-3">
          <div className="flex items-center gap-3">
            <HomeShortcut />
            <Link className="text-sm text-[var(--text-secondary)]" href="/categories">
              继续逛逛
            </Link>
          </div>
          <SearchForm />
        </div>

        {/* title */}
        <h1 className="py-2 text-lg font-semibold">购物车</h1>

        {/* list */}
        <CartList items={cart.items} />
      </div>

      {/* bottom bar */}
      <CartBottomBar summary={cart.summary} />
    </main>
  );
}
