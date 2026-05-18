import type { StorefrontCategory } from "@/lib/storefront/types";

export const HOMEPAGE_CATEGORY_NAMES = [
  "手机数码",
  "家用电器",
  "服饰鞋包",
  "美妆个护",
  "家居生活",
  "食品饮料",
  "母婴玩具",
  "运动户外",
  "汽车用品",
  "其他分类"
] as const;

export type HomepageCategoryName = (typeof HOMEPAGE_CATEGORY_NAMES)[number];

export type HomepageCategoryItem = {
  name: HomepageCategoryName;
  href: string;
  isFallback: boolean;
  categoryId?: number;
  unmatchedCount?: number;
};

export function buildHomepageCategoryItems(categories: StorefrontCategory[]): HomepageCategoryItem[] {
  const categoriesByName = new Map(categories.map((category) => [category.name, category]));
  const fixedCategorySet = new Set<string>(HOMEPAGE_CATEGORY_NAMES);
  const unmatchedCount = categories.filter((category) => !fixedCategorySet.has(category.name)).length;

  return HOMEPAGE_CATEGORY_NAMES.map((name) => {
    if (name === "其他分类") {
      return {
        name,
        href: searchHref(name),
        isFallback: unmatchedCount === 0,
        unmatchedCount
      };
    }

    const category = categoriesByName.get(name);
    if (category) {
      return {
        name,
        href: `/categories/${category.id}`,
        isFallback: false,
        categoryId: category.id
      };
    }

    return {
      name,
      href: searchHref(name),
      isFallback: true
    };
  });
}

function searchHref(keyword: string): string {
  return `/search?keyword=${encodeURIComponent(keyword)}`;
}
