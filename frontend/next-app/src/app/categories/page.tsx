import Link from "next/link";

import { CategoryDirectory } from "@/features/storefront/catalog/category-list";
import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { getSessionUser } from "@/lib/auth/server";
import { getServerStorefrontCategories } from "@/lib/storefront/server";

export default async function CategoriesPage() {
  const [categories, user] = await Promise.all([getServerStorefrontCategories(), getSessionUser()]);

  return (
    <main className="page-shell">
      <div className="page-stack">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-3">
            <HomeShortcut />
            {user ? (
              <>
                <Link className="btn-secondary px-4 py-2" href="/cart">
                  购物车
                </Link>
                <Link className="btn-secondary px-4 py-2" href="/account/addresses">
                  收货地址
                </Link>
              </>
            ) : null}
          </div>
          <SearchForm />
        </div>
        <CategoryDirectory categories={categories} />
      </div>
    </main>
  );
}
