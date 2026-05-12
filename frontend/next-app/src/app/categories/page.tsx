import { CategoryDirectory } from "@/features/storefront/catalog/category-list";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { getServerStorefrontCategories } from "@/lib/storefront/server";

export default async function CategoriesPage() {
  const categories = await getServerStorefrontCategories();

  return (
    <main className="min-h-screen px-6 py-10">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <SearchForm className="self-end" />
        <CategoryDirectory categories={categories} />
      </div>
    </main>
  );
}
