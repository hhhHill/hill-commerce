"use client";

import type {
  ApiErrorResponse,
  AutoCompleteResult,
  Category,
  CategoryStatus,
  CreateSalesInput,
  DashboardSummary,
  DisableResult,
  ProductDetail,
  ProductPayload,
  ProductStatus,
  ProductSummary,
  ResetPasswordInput,
  SalesUser,
  SalesUserListResult,
  ShipOrderResult
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

export async function listSalesUsers(): Promise<SalesUser[]> {
  const result = await sendAdminRequest<SalesUserListResult>("/api/admin/users", {
    method: "GET"
  });
  return result.users;
}

export async function createSalesUser(input: CreateSalesInput): Promise<SalesUser> {
  return sendAdminRequest<SalesUser>("/api/admin/users", {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function disableSalesUser(userId: number): Promise<DisableResult> {
  return sendAdminRequest<DisableResult>(`/api/admin/users/${userId}/disable`, {
    method: "POST"
  });
}

export async function enableSalesUser(userId: number): Promise<DisableResult> {
  return sendAdminRequest<DisableResult>(`/api/admin/users/${userId}/enable`, {
    method: "POST"
  });
}

export async function resetSalesPassword(userId: number, input: ResetPasswordInput): Promise<DisableResult> {
  return sendAdminRequest<DisableResult>(`/api/admin/users/${userId}/reset-password`, {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  return sendAdminRequest<DashboardSummary>("/api/admin/dashboard/summary", {
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
