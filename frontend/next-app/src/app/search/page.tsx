import { SearchForm } from "@/features/storefront/catalog/search-form";
import { StorefrontProductList } from "@/features/storefront/catalog/product-list";
import { searchServerStorefrontProducts } from "@/lib/storefront/server";

type SearchPageProps = {
  searchParams: Promise<{
    keyword?: string;
    page?: string;
  }>;
};

export default async function SearchPage({ searchParams }: SearchPageProps) {
  const { keyword = "", page } = await searchParams;
  const currentPage = Number(page ?? "1");
  const result = keyword.trim()
    ? await searchServerStorefrontProducts({
        keyword,
        page: Number.isNaN(currentPage) ? 1 : currentPage
      })
    : { items: [], page: 1, pageSize: 12, total: 0 };

  return (
    <main className="min-h-screen px-6 py-10">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <SearchForm className="self-end" defaultKeyword={keyword} />
        <StorefrontProductList
          buildPageHref={(nextPage) => `/search?keyword=${encodeURIComponent(keyword)}&page=${nextPage}`}
          emptyDescription={keyword.trim() ? `没有找到与“${keyword}”相关的商品。` : "请输入商品名称后开始搜索。"}
          page={result.page}
          pageSize={result.pageSize}
          products={result.items}
          total={result.total}
          title={keyword.trim() ? `搜索结果：${keyword}` : "商品搜索"}
        />
      </div>
    </main>
  );
}
