"use client";

import type {
  StorefrontRecommendationParams,
  StorefrontRecommendationResponse
} from "@/lib/storefront/recommendation-types";

export async function getStorefrontRecommendations(
  params: StorefrontRecommendationParams = {}
): Promise<StorefrontRecommendationResponse> {
  const response = await fetch(`/api/storefront/recommendations${buildRecommendationQuery(params)}`, {
    credentials: "include",
    cache: "no-store"
  });

  if (!response.ok) {
    throw new Error("推荐商品加载失败");
  }

  return (await response.json()) as StorefrontRecommendationResponse;
}

function buildRecommendationQuery(params: StorefrontRecommendationParams): string {
  const search = new URLSearchParams();
  if (params.type) {
    search.set("type", params.type);
  }
  if (params.productId) {
    search.set("productId", String(params.productId));
  }
  if (params.n) {
    search.set("n", String(params.n));
  }

  const query = search.toString();
  return query ? `?${query}` : "";
}
