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
    <section>
      {/* Desktop: vertical sidebar */}
      <div className="hidden flex-col overflow-hidden rounded-xl bg-white md:flex">
        <div className="border-b border-[var(--border-normal)] px-3 py-3">
          <h2 className="text-sm font-bold text-[var(--text-primary)]">全部分类</h2>
        </div>
        <nav className="flex flex-col">
          {items.map((item) => (
            <CategoryLink className="px-3 py-3" item={item} key={item.name} />
          ))}
        </nav>
      </div>

      {/* Mobile: horizontal scroll */}
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
      className={`group flex items-center justify-between gap-2 text-sm text-[var(--text-secondary)] transition-colors hover:bg-[#fff2ec] hover:text-[var(--brand-primary)] ${className}`}
      eventName={STOREFRONT_BROWSE_EVENTS.categoryEnter}
      eventPayload={{ categoryId: item.categoryId ?? item.name, source: "category-directory" }}
      href={item.href}
    >
      <span className="truncate font-medium">{item.name}</span>
      <span className="text-[11px] text-[var(--text-hint)] group-hover:text-[var(--brand-primary)]">
        {item.name === "其他分类" && item.unmatchedCount ? `${item.unmatchedCount}+` : "›"}
      </span>
    </BrowseEventLink>
  );
}
