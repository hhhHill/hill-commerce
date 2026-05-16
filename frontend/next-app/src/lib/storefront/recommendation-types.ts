import type { StorefrontProductCard } from "@/lib/storefront/types";

export type StorefrontRecommendationType = "home" | "detail";

export type StorefrontRecommendationParams = {
  type?: StorefrontRecommendationType;
  productId?: number;
  n?: number;
};

export type StorefrontRecommendationResponse = {
  items: StorefrontProductCard[];
};
