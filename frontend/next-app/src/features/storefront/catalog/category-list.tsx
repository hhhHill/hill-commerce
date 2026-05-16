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
    <section className="flex flex-col gap-3">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">品类入口</h1>
        <p className="text-sm text-[var(--text-secondary)]">左右滑动，快速进入你想逛的分类。</p>
      </div>
      <div className="-mx-3 overflow-x-auto px-3 [scrollbar-width:none]">
        <div className="flex min-w-max gap-2">
          {categories.map((category) => (
          <BrowseEventLink
            key={category.id}
            className="surface-card group flex min-h-20 min-w-[92px] shrink-0 flex-col justify-between rounded-lg px-3 py-3"
            eventName={STOREFRONT_BROWSE_EVENTS.categoryEnter}
            eventPayload={{ categoryId: category.id, source: "category-directory" }}
            href={`/categories/${category.id}`}
          >
            <span className="text-xs font-semibold text-[var(--brand-primary)]">分类</span>
            <div className="flex items-end justify-between gap-3">
              <h2 className="line-clamp-2 text-sm font-semibold text-[var(--text-primary)]">{category.name}</h2>
              <span className="text-xs font-medium text-[var(--text-hint)] transition-transform duration-200 group-hover:translate-x-0.5">逛</span>
            </div>
          </BrowseEventLink>
          ))}
        </div>
      </div>
    </section>
  );
}
