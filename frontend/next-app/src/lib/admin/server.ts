import { cookies } from "next/headers";

import { getBackendBaseUrl } from "@/lib/config";
import type {
  AggregateProfileResponse,
  ProductRankingResponse,
  TrendResponse
} from "@/lib/admin/analytics-types";
import type {
  AdminOrderListResult,
  Category,
  DashboardSummary,
  LoginLogListResult,
  OperationLogListResult,
  ProductDetail,
  ProductListFilters,
  ProductSummary,
  ProductViewLogListResult,
  SalesUserListResult
} from "@/lib/admin/types";
import type { OrderDetail, OrderListStatus } from "@/lib/order/types";

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

export async function getAdminOrders(status?: OrderListStatus, page = 1, size = 10): Promise<AdminOrderListResult> {
  const search = new URLSearchParams();
  if (status) {
    search.set("status", status);
  }
  if (page > 1) {
    search.set("page", String(page));
  }
  if (size !== 10) {
    search.set("size", String(size));
  }
  const query = search.toString();
  return fetchAdminJson<AdminOrderListResult>(`/api/admin/orders${query ? `?${query}` : ""}`);
}

export async function getAdminShipOrder(orderId: number): Promise<OrderDetail> {
  return fetchAdminJson<OrderDetail>(`/api/admin/orders/${orderId}/ship`);
}

export async function getServerSalesUsers() {
  const result = await fetchAdminJson<SalesUserListResult>("/api/admin/users");
  return result.users;
}

export async function getServerDashboardSummary(): Promise<DashboardSummary> {
  return fetchAdminJson<DashboardSummary>("/api/admin/dashboard/summary");
}

export async function getServerAnalyticsTrends(params: { granularity?: string; from?: string; to?: string } = {}): Promise<TrendResponse> {
  const search = new URLSearchParams();
  if (params.granularity) {
    search.set("granularity", params.granularity);
  }
  if (params.from) {
    search.set("from", params.from);
  }
  if (params.to) {
    search.set("to", params.to);
  }
  const query = search.toString();
  return fetchAdminJson<TrendResponse>(`/api/admin/analytics/trends${query ? `?${query}` : ""}`);
}

export async function getServerAnalyticsProductRankings(params: { range?: string; limit?: number } = {}): Promise<ProductRankingResponse> {
  const search = new URLSearchParams();
  if (params.range) {
    search.set("range", params.range);
  }
  if (params.limit) {
    search.set("limit", String(params.limit));
  }
  const query = search.toString();
  return fetchAdminJson<ProductRankingResponse>(`/api/admin/analytics/rankings/products${query ? `?${query}` : ""}`);
}

export async function getServerAnalyticsAggregateProfiles(): Promise<AggregateProfileResponse> {
  return fetchAdminJson<AggregateProfileResponse>("/api/admin/analytics/profiles/aggregate");
}

export async function getServerLoginLogs(params: { email?: string; result?: string } = {}): Promise<LoginLogListResult> {
  const search = new URLSearchParams();
  if (params.email) {
    search.set("email", params.email);
  }
  if (params.result) {
    search.set("result", params.result);
  }
  const query = search.toString();
  return fetchAdminJson<LoginLogListResult>(`/api/admin/login-logs${query ? `?${query}` : ""}`);
}

export async function getServerOperationLogs(
  params: { operatorUserId?: number; actionType?: string } = {}
): Promise<OperationLogListResult> {
  const search = new URLSearchParams();
  if (params.operatorUserId) {
    search.set("operatorUserId", String(params.operatorUserId));
  }
  if (params.actionType) {
    search.set("actionType", params.actionType);
  }
  const query = search.toString();
  return fetchAdminJson<OperationLogListResult>(`/api/admin/operation-logs${query ? `?${query}` : ""}`);
}

export async function getServerViewLogs(params: { productId?: number; categoryId?: number } = {}): Promise<ProductViewLogListResult> {
  const search = new URLSearchParams();
  if (params.productId) {
    search.set("productId", String(params.productId));
  }
  if (params.categoryId) {
    search.set("categoryId", String(params.categoryId));
  }
  const query = search.toString();
  return fetchAdminJson<ProductViewLogListResult>(`/api/admin/view-logs${query ? `?${query}` : ""}`);
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
