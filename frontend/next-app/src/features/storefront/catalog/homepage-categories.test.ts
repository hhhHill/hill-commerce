import { describe, expect, it } from "vitest";

import { buildHomepageCategoryItems, HOMEPAGE_CATEGORY_NAMES } from "./homepage-categories";
import type { StorefrontCategory } from "@/lib/storefront/types";

describe("buildHomepageCategoryItems", () => {
  it("returns fixed homepage categories in approved order", () => {
    const categories: StorefrontCategory[] = [
      { id: 3, name: "服饰鞋包" },
      { id: 11, name: "宠物用品" },
      { id: 1, name: "手机数码" }
    ];

    const items = buildHomepageCategoryItems(categories);

    expect(items.map((item) => item.name)).toEqual(HOMEPAGE_CATEGORY_NAMES);
    expect(items[0]).toMatchObject({ name: "手机数码", href: "/categories/1", isFallback: false });
    expect(items[2]).toMatchObject({ name: "服饰鞋包", href: "/categories/3", isFallback: false });
    expect(items[9]).toMatchObject({ name: "其他分类", href: "/search?keyword=%E5%85%B6%E4%BB%96%E5%88%86%E7%B1%BB", isFallback: false });
  });

  it("links unmatched fixed categories to search fallback", () => {
    const items = buildHomepageCategoryItems([{ id: 8, name: "运动户外" }]);

    expect(items[0]).toMatchObject({ name: "手机数码", href: "/search?keyword=%E6%89%8B%E6%9C%BA%E6%95%B0%E7%A0%81", isFallback: true });
    expect(items[7]).toMatchObject({ name: "运动户外", href: "/categories/8", isFallback: false });
  });
});
