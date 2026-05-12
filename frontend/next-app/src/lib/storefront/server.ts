import { getBackendBaseUrl } from "@/lib/config";
import { StorefrontRequestError } from "@/lib/storefront/errors";
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

async function fetchStorefrontJson<T>(pathname: string): Promise<T> {
  const response = await fetch(`${getBackendBaseUrl()}${pathname}`, {
    method: "GET",
    cache: "no-store",
    redirect: "manual"
  });

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
