import { cookies } from "next/headers";

import { getBackendBaseUrl } from "@/lib/config";
import type { Category, ProductDetail, ProductListFilters, ProductSummary } from "@/lib/admin/types";

export async function getAdminCategories(): Promise<Category[]> {
  return fetchAdminJson<Category[]>("/api/admin/categories");
}

export async function getAdminProducts(filters: ProductListFilters = {}): Promise<ProductSummary[]> {
  const search = new URLSearchParams();
  if (filters.name) {
    search.set("name", filters.name);
  }
  if (filters.categoryId) {
    search.set("categoryId", filters.categoryId);
  }
  if (filters.status) {
    search.set("status", filters.status);
  }

  const query = search.toString();
  return fetchAdminJson<ProductSummary[]>(`/api/admin/products${query ? `?${query}` : ""}`);
}

export async function getAdminProduct(productId: number): Promise<ProductDetail> {
  return fetchAdminJson<ProductDetail>(`/api/admin/products/${productId}`);
}

async function fetchAdminJson<T>(pathname: string): Promise<T> {
  const cookieHeader = await getCookieHeader();
  const response = await fetch(`${getBackendBaseUrl()}${pathname}`, {
    method: "GET",
    headers: cookieHeader ? { cookie: cookieHeader } : undefined,
    cache: "no-store",
    redirect: "manual"
  });

  if (!response.ok) {
    throw new Error(`Failed to load admin data: ${response.status}`);
  }

  return (await response.json()) as T;
}

async function getCookieHeader(): Promise<string> {
  const cookieStore = await cookies();
  return cookieStore
    .getAll()
    .map(({ name, value }) => `${name}=${value}`)
    .join("; ");
}
