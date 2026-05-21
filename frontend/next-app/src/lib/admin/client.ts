"use client";

import type {
  ApiErrorResponse,
  AutoCompleteResult,
  Category,
  CategoryStatus,
  CreateMerchantInput,
  DashboardSummary,
  DisableResult,
  ProductDetail,
  LoginLogListResult,
  MerchantUser,
  MerchantUserListResult,
  OperationLogListResult,
  ProductPayload,
  ProductViewLogListResult,
  ProductStatus,
  ProductSummary,
  ResetPasswordInput,
  Shop,
  ShipOrderResult,
  UpdateShopRequest
} from "@/lib/admin/types";

type CategoryInput = {
  name: string;
  sortOrder: number;
  status: CategoryStatus;
};

export async function createCategory(input: CategoryInput): Promise<Category> {
  return sendAdminRequest<Category>("/api/admin/categories", {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function updateCategory(categoryId: number, input: CategoryInput): Promise<Category> {
  return sendAdminRequest<Category>(`/api/admin/categories/${categoryId}`, {
    method: "PUT",
    body: JSON.stringify(input)
  });
}

export async function deleteCategory(categoryId: number): Promise<void> {
  await sendAdminRequest(`/api/admin/categories/${categoryId}`, {
    method: "DELETE"
  });
}

export async function createProduct(input: ProductPayload): Promise<ProductDetail> {
  return sendAdminRequest<ProductDetail>("/api/admin/products", {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function updateProduct(productId: number, input: ProductPayload): Promise<ProductDetail> {
  return sendAdminRequest<ProductDetail>(`/api/admin/products/${productId}`, {
    method: "PUT",
    body: JSON.stringify(input)
  });
}

export async function updateProductStatus(productId: number, status: ProductStatus): Promise<ProductSummary> {
  return sendAdminRequest<ProductSummary>(`/api/admin/products/${productId}/status`, {
    method: "PUT",
    body: JSON.stringify({ status })
  });
}

export async function deleteProduct(productId: number): Promise<void> {
  await sendAdminRequest(`/api/admin/products/${productId}`, {
    method: "DELETE"
  });
}

export async function shipOrder(orderId: number, carrierName: string, trackingNo: string): Promise<ShipOrderResult> {
  return sendAdminRequest<ShipOrderResult>(`/api/admin/orders/${orderId}/ship`, {
    method: "POST",
    body: JSON.stringify({ carrierName, trackingNo })
  });
}

export async function triggerAutoComplete(): Promise<AutoCompleteResult> {
  return sendAdminRequest<AutoCompleteResult>("/api/admin/orders/auto-complete", {
    method: "POST"
  });
}

export async function listMerchantUsers(): Promise<MerchantUser[]> {
  const result = await sendAdminRequest<MerchantUserListResult>("/api/admin/users", {
    method: "GET"
  });
  return result.users;
}

export async function createMerchantUser(input: CreateMerchantInput): Promise<MerchantUser> {
  return sendAdminRequest<MerchantUser>("/api/admin/users", {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function disableMerchantUser(userId: number): Promise<DisableResult> {
  return sendAdminRequest<DisableResult>(`/api/admin/users/${userId}/disable`, {
    method: "POST"
  });
}

export async function enableMerchantUser(userId: number): Promise<DisableResult> {
  return sendAdminRequest<DisableResult>(`/api/admin/users/${userId}/enable`, {
    method: "POST"
  });
}

export async function resetMerchantPassword(userId: number, input: ResetPasswordInput): Promise<DisableResult> {
  return sendAdminRequest<DisableResult>(`/api/admin/users/${userId}/reset-password`, {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function getMyShop(): Promise<Shop> {
  return sendAdminRequest<Shop>("/api/admin/shop", {
    method: "GET"
  });
}

export async function updateMyShop(req: UpdateShopRequest): Promise<Shop> {
  return sendAdminRequest<Shop>("/api/admin/shop", {
    method: "PUT",
    body: JSON.stringify(req)
  });
}

export async function listShops(page = 1, size = 20): Promise<{ items: Shop[]; total: number }> {
  return sendAdminRequest<{ items: Shop[]; total: number }>(`/api/admin/shops?page=${page}&size=${size}`, {
    method: "GET"
  });
}

export async function disableShop(id: number): Promise<void> {
  await sendAdminRequest(`/api/admin/shops/${id}/disable`, { method: "POST" });
}

export async function enableShop(id: number): Promise<void> {
  await sendAdminRequest(`/api/admin/shops/${id}/enable`, { method: "POST" });
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  return sendAdminRequest<DashboardSummary>("/api/admin/dashboard/summary", {
    method: "GET"
  });
}

export async function getLoginLogs(params: { email?: string; result?: string } = {}): Promise<LoginLogListResult> {
  const search = new URLSearchParams();
  if (params.email) {
    search.set("email", params.email);
  }
  if (params.result) {
    search.set("result", params.result);
  }

  const query = search.toString();
  return sendAdminRequest<LoginLogListResult>(`/api/admin/login-logs${query ? `?${query}` : ""}`, {
    method: "GET"
  });
}

export async function getOperationLogs(
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
  return sendAdminRequest<OperationLogListResult>(`/api/admin/operation-logs${query ? `?${query}` : ""}`, {
    method: "GET"
  });
}

export async function getViewLogs(params: { productId?: number; categoryId?: number } = {}): Promise<ProductViewLogListResult> {
  const search = new URLSearchParams();
  if (params.productId) {
    search.set("productId", String(params.productId));
  }
  if (params.categoryId) {
    search.set("categoryId", String(params.categoryId));
  }

  const query = search.toString();
  return sendAdminRequest<ProductViewLogListResult>(`/api/admin/view-logs${query ? `?${query}` : ""}`, {
    method: "GET"
  });
}

async function sendAdminRequest<T>(input: RequestInfo, init: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    credentials: "include",
    headers: {
      "content-type": "application/json",
      ...(init.headers ?? {})
    }
  });

  if (!response.ok) {
    const payload = (await safeJson(response)) as ApiErrorResponse | null;
    throw new Error(payload?.message ?? "请求失败，请稍后重试");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function safeJson(response: Response): Promise<unknown | null> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
