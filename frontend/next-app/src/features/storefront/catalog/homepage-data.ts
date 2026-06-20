import {
  getServerActivityCards,
  getServerStorefrontCategories,
  getServerStorefrontProducts,
  getServerStorefrontRecommendations,
  type StorefrontActivityCard
} from "@/lib/storefront/server";
import type { StorefrontCategory, StorefrontPagedResponse, StorefrontProductCard } from "@/lib/storefront/types";

export type HomepageData = {
  categories: StorefrontCategory[];
  products: StorefrontPagedResponse<StorefrontProductCard>;
  recommendations: StorefrontProductCard[];
  activityCards: StorefrontActivityCard[];
};

type HomepageDataLoaders = {
  getCategories: () => Promise<StorefrontCategory[]>;
  getProducts: () => Promise<StorefrontPagedResponse<StorefrontProductCard>>;
  getRecommendations: () => Promise<StorefrontProductCard[]>;
  getActivityCards: () => Promise<StorefrontActivityCard[]>;
};

const DEFAULT_PRODUCTS_PAGE: StorefrontPagedResponse<StorefrontProductCard> = {
  items: [],
  page: 1,
  pageSize: 24,
  total: 0
};

const DEFAULT_LOADERS: HomepageDataLoaders = {
  getCategories: getServerStorefrontCategories,
  getProducts: () => getServerStorefrontProducts({ pageSize: 24 }),
  getRecommendations: () => getServerStorefrontRecommendations({ type: "home", n: 10 }),
  getActivityCards: () => getServerActivityCards()
};

export async function loadHomepageData(loaders: HomepageDataLoaders = DEFAULT_LOADERS): Promise<HomepageData> {
  const [categories, products, recommendations, activityCards] = await Promise.allSettled([
    loaders.getCategories(),
    loaders.getProducts(),
    loaders.getRecommendations(),
    loaders.getActivityCards()
  ]);

  return {
    categories: categories.status === "fulfilled" ? categories.value : [],
    products: products.status === "fulfilled" ? products.value : DEFAULT_PRODUCTS_PAGE,
    recommendations: recommendations.status === "fulfilled" ? recommendations.value : [],
    activityCards: activityCards.status === "fulfilled" ? activityCards.value : []
  };
}
