import { notFound } from "next/navigation";

import { SearchForm } from "@/features/storefront/catalog/search-form";
import { StorefrontProductList } from "@/features/storefront/catalog/product-list";
import { StorefrontRequestError } from "@/lib/storefront/errors";
import { getServerStorefrontCategoryProducts } from "@/lib/storefront/server";

type CategoryProductsPageProps = {
  params: Promise<{
    categoryId: string;
  }>;
  searchParams: Promise<{
    page?: string;
  }>;
};

export default async function CategoryProductsPage({ params, searchParams }: CategoryProductsPageProps) {
  const { categoryId } = await params;
  const { page } = await searchParams;
  const currentPage = Number(page ?? "1");
  try {
    const result = await getServerStorefrontCategoryProducts(Number(categoryId), {
      page: Number.isNaN(currentPage) ? 1 : currentPage
    });

    return (
      <main className="min-h-screen px-6 py-10">
        <div className="mx-auto flex max-w-6xl flex-col gap-8">
          <SearchForm className="self-end" />
          <StorefrontProductList
            buildPageHref={(nextPage) => `/categories/${categoryId}?page=${nextPage}`}
            emptyDescription="这个分类下暂时还没有可浏览的商品。"
            page={result.page}
            pageSize={result.pageSize}
            products={result.items}
            total={result.total}
            title="分类商品"
          />
        </div>
      </main>
    );
  } catch (error) {
    if (error instanceof StorefrontRequestError && error.status === 404) {
      notFound();
    }

    throw error;
  }
}
