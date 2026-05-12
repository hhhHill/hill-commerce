import { BrowseEventLink } from "@/features/storefront/catalog/browse-event-link";
import { EmptyState } from "@/features/storefront/catalog/empty-state";
import { STOREFRONT_BROWSE_EVENTS } from "@/lib/storefront/logging";
import type { StorefrontCategory } from "@/lib/storefront/types";

type CategoryDirectoryProps = {
  categories: StorefrontCategory[];
};

export function CategoryDirectory({ categories }: CategoryDirectoryProps) {
  if (categories.length === 0) {
    return <EmptyState description="当前还没有可浏览的分类，你可以稍后再来看看。" title="分类暂未开放" actionHref="/" actionLabel="返回首页" />;
  }

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <span className="text-xs font-semibold uppercase tracking-[0.22em] text-black/45">Category Directory</span>
        <h1 className="text-4xl font-semibold tracking-tight">按分类开始浏览</h1>
      </div>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {categories.map((category, index) => (
          <BrowseEventLink
            key={category.id}
            className="group flex min-h-40 flex-col justify-between rounded-[28px] border border-black/10 px-6 py-6 shadow-[0_16px_40px_rgba(74,42,18,0.08)]"
            eventName={STOREFRONT_BROWSE_EVENTS.categoryEnter}
            eventPayload={{ categoryId: category.id, source: "category-directory" }}
            href={`/categories/${category.id}`}
            style={{
              background:
                index % 3 === 0
                  ? "linear-gradient(160deg, #fff7ef 0%, #f0dbc1 100%)"
                  : index % 3 === 1
                    ? "linear-gradient(160deg, #f6efe4 0%, #dfcbb4 100%)"
                    : "linear-gradient(160deg, #fef3dc 0%, #efcf9a 100%)"
            }}
          >
            <span className="rounded-full bg-white/60 px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">Category</span>
            <div className="flex items-end justify-between gap-4">
              <h2 className="text-2xl font-semibold tracking-tight">{category.name}</h2>
              <span className="text-sm font-medium text-black/50 transition-transform duration-200 group-hover:translate-x-1">进入</span>
            </div>
          </BrowseEventLink>
        ))}
      </div>
    </section>
  );
}
