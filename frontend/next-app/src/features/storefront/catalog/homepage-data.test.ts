import { describe, expect, it, vi } from "vitest";

import { loadHomepageData } from "@/features/storefront/catalog/homepage-data";

describe("loadHomepageData", () => {
  it("returns safe fallbacks when upstream storefront requests fail", async () => {
    const data = await loadHomepageData({
      getCategories: vi.fn().mockRejectedValue(new Error("categories failed")),
      getProducts: vi.fn().mockRejectedValue(new Error("products failed")),
      getRecommendations: vi.fn().mockRejectedValue(new Error("recommendations failed")),
      getActivityCards: vi.fn().mockRejectedValue(new Error("activity cards failed"))
    });

    expect(data.categories).toEqual([]);
    expect(data.products).toEqual({
      items: [],
      page: 1,
      pageSize: 24,
      total: 0
    });
    expect(data.recommendations).toEqual([]);
    expect(data.activityCards).toEqual([]);
  });

  it("preserves successful upstream data", async () => {
    const data = await loadHomepageData({
      getCategories: vi.fn().mockResolvedValue([{ id: 1, name: "手机" }]),
      getProducts: vi.fn().mockResolvedValue({
        items: [{ id: 1, categoryId: 1, name: "旗舰机", salePrice: "4999.00", coverImageUrl: null }],
        page: 1,
        pageSize: 24,
        total: 1
      }),
      getRecommendations: vi.fn().mockResolvedValue([
        { id: 2, categoryId: 1, name: "推荐商品", salePrice: "3999.00", coverImageUrl: null }
      ]),
      getActivityCards: vi.fn().mockResolvedValue([{ title: "限时秒杀", imageUrl: null, linkUrl: "/search?keyword=秒杀" }])
    });

    expect(data.categories).toEqual([{ id: 1, name: "手机" }]);
    expect(data.products.total).toBe(1);
    expect(data.recommendations).toHaveLength(1);
    expect(data.activityCards).toHaveLength(1);
  });
});
