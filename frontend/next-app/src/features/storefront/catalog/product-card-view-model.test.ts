import { describe, expect, it } from "vitest";

import { buildProductCardViewModel } from "./product-card-view-model";
import type { StorefrontProductCard } from "@/lib/storefront/types";

const product: StorefrontProductCard = {
  id: 42,
  categoryId: 3,
  name: "春夏薄款宽松短袖",
  salePrice: "39.90",
  coverImageUrl: null
};

describe("buildProductCardViewModel", () => {
  it("derives stable ecommerce display fields from a product card", () => {
    const first = buildProductCardViewModel(product);
    const second = buildProductCardViewModel(product);

    expect(second).toEqual(first);
    expect(first.price).toBe("39.90");
    expect(first.originalPrice).toMatch(/^\d+\.\d{2}$/);
    expect(Number(first.originalPrice)).toBeGreaterThan(39.9);
    expect(first.sales).toMatch(/^已售 \d+(?:\.\d)?[w万]?\+$/);
    expect(first.shopName).toBeTruthy();
    expect(first.tags.length).toBeGreaterThanOrEqual(2);
    expect(first.tags.length).toBeLessThanOrEqual(3);
  });

  it("keeps null prices as display fallback without producing original price", () => {
    const viewModel = buildProductCardViewModel({ ...product, salePrice: null });

    expect(viewModel.price).toBe("待定价");
    expect(viewModel.originalPrice).toBeNull();
  });
});
