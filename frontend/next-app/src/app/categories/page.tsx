import Link from "next/link";

import { CategoryDirectory } from "@/features/storefront/catalog/category-list";
import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { getSessionUser } from "@/lib/auth/server";
import { getServerStorefrontCategories } from "@/lib/storefront/server";

export default async function CategoriesPage() {
  const [categories, user] = await Promise.all([getServerStorefrontCategories(), getSessionUser()]);

  return (
    <main className="min-h-screen px-6 py-10">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-3">
            <HomeShortcut />
            {user ? (
              <>
                <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/cart">
                  购物车
                </Link>
                <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/account/addresses">
                  收货地址
                </Link>
              </>
            ) : null}
          </div>
          <SearchForm className="w-full max-w-md" />
        </div>
        <CategoryDirectory categories={categories} />
      </div>
    </main>
  );
}
