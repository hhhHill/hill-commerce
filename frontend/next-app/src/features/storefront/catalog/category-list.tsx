import { BrowseEventLink } from "@/features/storefront/catalog/browse-event-link";
import { buildHomepageCategoryItems } from "@/features/storefront/catalog/homepage-categories";
import { STOREFRONT_BROWSE_EVENTS } from "@/lib/storefront/logging";
import type { StorefrontCategory } from "@/lib/storefront/types";

type CategoryDirectoryProps = {
  categories: StorefrontCategory[];
};

export function CategoryDirectory({ categories }: CategoryDirectoryProps) {
  const items = buildHomepageCategoryItems(categories);

  return (
    <section className="md:sticky md:top-24 md:self-start">
      <div className="hidden flex-col gap-2 rounded-lg border border-[var(--border-normal)] bg-white p-2 shadow-[0_2px_8px_rgba(0,0,0,0.04)] md:flex">
        <div className="px-2 pb-1 pt-1">
          <h2 className="text-sm font-bold text-[var(--text-primary)]">全部分类</h2>
          <p className="mt-0.5 text-[11px] text-[var(--text-hint)]">固定频道入口</p>
        </div>
        <nav className="flex flex-col gap-1">
          {items.map((item) => (
            <CategoryLink className="min-h-9 px-2.5 py-2" item={item} key={item.name} />
          ))}
        </nav>
      </div>

      <div className="md:hidden">
        <div className="-mx-3 overflow-x-auto px-3 [scrollbar-width:none]">
          <nav className="flex min-w-max gap-2">
            {items.map((item) => (
              <CategoryLink className="min-w-[86px] px-3 py-2.5" item={item} key={item.name} />
            ))}
          </nav>
        </div>
      </div>
    </section>
  );
}

type CategoryLinkProps = {
  item: ReturnType<typeof buildHomepageCategoryItems>[number];
  className: string;
};

function CategoryLink({ item, className }: CategoryLinkProps) {
  return (
    <BrowseEventLink
      className={`group flex items-center justify-between gap-2 rounded-lg border border-transparent bg-white text-sm font-semibold text-[var(--text-primary)] transition hover:border-[#ffd5c2] hover:bg-[#fff2ec] hover:text-[#ff4400] ${className}`}
      eventName={STOREFRONT_BROWSE_EVENTS.categoryEnter}
      eventPayload={{ categoryId: item.categoryId ?? item.name, source: "category-directory" }}
      href={item.href}
    >
      <span className="truncate">{item.name}</span>
      <span className="text-[11px] font-medium text-[var(--text-hint)] group-hover:text-[#ff4400]">
        {item.name === "其他分类" && item.unmatchedCount ? `${item.unmatchedCount}+` : "逛"}
      </span>
    </BrowseEventLink>
  );
}
