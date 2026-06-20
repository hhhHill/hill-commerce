import { cookies } from "next/headers";

import { getBackendBaseUrl } from "@/lib/config";
import { fetchWithTimeout, isTimeoutError } from "@/lib/server/fetch-with-timeout";
import { StorefrontRequestError } from "@/lib/storefront/errors";
import type {
  StorefrontRecommendationParams,
  StorefrontRecommendationResponse
} from "@/lib/storefront/recommendation-types";
import type {
  StorefrontCategory,
  StorefrontListParams,
  StorefrontPagedResponse,
  StorefrontProductCard,
  StorefrontProductDetail,
  StorefrontSearchParams
} from "@/lib/storefront/types";

export async function getServerStorefrontCategories(): Promise<StorefrontCategory[]> {
  return fetchStorefrontJson<StorefrontCategory[]>("/api/categories");
}

export async function getServerStorefrontProducts(
  params: StorefrontListParams = {}
): Promise<StorefrontPagedResponse<StorefrontProductCard>> {
  return fetchStorefrontJson<StorefrontPagedResponse<StorefrontProductCard>>(`/api/products${buildListQuery(params)}`);
}

export async function getServerStorefrontCategoryProducts(
  categoryId: number,
  params: StorefrontListParams = {}
): Promise<StorefrontPagedResponse<StorefrontProductCard>> {
  return fetchStorefrontJson<StorefrontPagedResponse<StorefrontProductCard>>(
    `/api/categories/${categoryId}/products${buildListQuery(params)}`
  );
}

export async function searchServerStorefrontProducts(
  params: StorefrontSearchParams
): Promise<StorefrontPagedResponse<StorefrontProductCard>> {
  const search = new URLSearchParams();
  search.set("keyword", params.keyword);
  if (params.page) {
    search.set("page", String(params.page));
  }
  if (params.pageSize) {
    search.set("pageSize", String(params.pageSize));
  }

  return fetchStorefrontJson<StorefrontPagedResponse<StorefrontProductCard>>(`/api/search?${search.toString()}`);
}

export async function getServerStorefrontProductDetail(productId: number): Promise<StorefrontProductDetail> {
  return fetchStorefrontJson<StorefrontProductDetail>(`/api/products/${productId}`);
}

export type StorefrontActivityCard = {
  title: string;
  imageUrl: string | null;
  linkUrl: string;
};

export async function getServerActivityCards(placement = "homepage"): Promise<StorefrontActivityCard[]> {
  try {
    const response = await fetchStorefrontJson<{ items: StorefrontActivityCard[] }>(
      `/api/storefront/activity-cards?placement=${encodeURIComponent(placement)}`
    );
    return response.items;
  } catch {
    return [];
  }
}

export async function getServerStorefrontRecommendations(
  params: StorefrontRecommendationParams = {}
): Promise<StorefrontProductCard[]> {
  try {
    const response = await fetchStorefrontJson<StorefrontRecommendationResponse>(
      `/api/storefront/recommendations${buildRecommendationQuery(params)}`,
      await getCookieHeader()
    );

    return response.items;
  } catch {
    return [];
  }
}

async function fetchStorefrontJson<T>(pathname: string, cookieHeader?: string): Promise<T> {
  let response: Response;
  try {
    response = await fetchWithTimeout(`${getBackendBaseUrl()}${pathname}`, {
      method: "GET",
      headers: cookieHeader ? { cookie: cookieHeader } : undefined,
      cache: "no-store",
      redirect: "manual"
    });
  } catch (error) {
    if (isTimeoutError(error)) {
      throw new StorefrontRequestError(504, `Timed out loading storefront data for ${pathname}`);
    }
    if (error instanceof TypeError) {
      throw new StorefrontRequestError(502, `Failed to reach storefront backend for ${pathname}`);
    }
    throw error;
  }

  if (!response.ok) {
    throw new StorefrontRequestError(response.status, `Failed to load storefront data: ${response.status}`);
  }

  return (await response.json()) as T;
}

function buildListQuery(params: StorefrontListParams): string {
  const search = new URLSearchParams();
  if (params.page) {
    search.set("page", String(params.page));
  }
  if (params.pageSize) {
    search.set("pageSize", String(params.pageSize));
  }

  const query = search.toString();
  return query ? `?${query}` : "";
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

async function getCookieHeader(): Promise<string> {
  const cookieStore = await cookies();

  return cookieStore
    .getAll()
    .map(({ name, value }) => `${name}=${value}`)
    .join("; ");
}
