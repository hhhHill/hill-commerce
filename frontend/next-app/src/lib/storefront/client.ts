"use client";

import type {
  ApiErrorResponse,
  StorefrontCategory,
  StorefrontListParams,
  StorefrontPagedResponse,
  StorefrontProductCard,
  StorefrontProductDetail,
  StorefrontSearchParams
} from "@/lib/storefront/types";

export async function getStorefrontCategories(): Promise<StorefrontCategory[]> {
  return sendStorefrontRequest<StorefrontCategory[]>("/api/categories");
}

export async function getStorefrontProducts(params: StorefrontListParams = {}): Promise<StorefrontPagedResponse<StorefrontProductCard>> {
  return sendStorefrontRequest<StorefrontPagedResponse<StorefrontProductCard>>(`/api/products${buildListQuery(params)}`);
}

export async function getStorefrontCategoryProducts(
  categoryId: number,
  params: StorefrontListParams = {}
): Promise<StorefrontPagedResponse<StorefrontProductCard>> {
  return sendStorefrontRequest<StorefrontPagedResponse<StorefrontProductCard>>(
    `/api/categories/${categoryId}/products${buildListQuery(params)}`
  );
}

export async function searchStorefrontProducts(
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

  return sendStorefrontRequest<StorefrontPagedResponse<StorefrontProductCard>>(`/api/search?${search.toString()}`);
}

export async function getStorefrontProductDetail(productId: number): Promise<StorefrontProductDetail> {
  return sendStorefrontRequest<StorefrontProductDetail>(`/api/products/${productId}`);
}

async function sendStorefrontRequest<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    credentials: "include",
    headers: {
      "content-type": "application/json",
      ...(init?.headers ?? {})
    }
  });

  if (!response.ok) {
    const payload = (await safeJson(response)) as ApiErrorResponse | null;
    throw new Error(payload?.message ?? "请求失败，请稍后重试");
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

async function safeJson(response: Response): Promise<unknown | null> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
