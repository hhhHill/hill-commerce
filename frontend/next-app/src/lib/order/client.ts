"use client";

import { OrderRequestError } from "@/lib/order/errors";
import type {
  ApiErrorResponse,
  CancelOrderResult,
  ConfirmReceiptResult,
  CreateOrderResult,
  OrderCheckout,
  OrderDetail,
  OrderListQuery,
  OrderListResult
} from "@/lib/order/types";

export async function listOrders(query?: OrderListQuery): Promise<OrderListResult> {
  return sendOrderRequest<OrderListResult>(`/api/orders${buildOrderListSearch(query)}`);
}

export async function getOrderCheckout(): Promise<OrderCheckout> {
  return sendOrderRequest<OrderCheckout>("/api/orders/checkout");
}

export async function createOrder(): Promise<CreateOrderResult> {
  return sendOrderRequest<CreateOrderResult>("/api/orders", {
    method: "POST"
  });
}

export async function getOrder(orderId: number): Promise<OrderDetail> {
  return sendOrderRequest<OrderDetail>(`/api/orders/${orderId}`);
}

export async function cancelOrder(orderId: number): Promise<CancelOrderResult> {
  return sendOrderRequest<CancelOrderResult>(`/api/orders/${orderId}/cancel`, {
    method: "POST"
  });
}

export async function confirmReceipt(orderId: number): Promise<ConfirmReceiptResult> {
  return sendOrderRequest<ConfirmReceiptResult>(`/api/orders/${orderId}/receive`, {
    method: "POST"
  });
}

async function sendOrderRequest<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
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
    throw new OrderRequestError(response.status, payload?.message ?? "请求失败，请稍后重试");
  }

  return (await response.json()) as T;
}

function buildOrderListSearch(query?: OrderListQuery): string {
  if (!query) {
    return "";
  }

  const searchParams = new URLSearchParams();
  if (query.page !== undefined) {
    searchParams.set("page", String(query.page));
  }
  if (query.size !== undefined) {
    searchParams.set("size", String(query.size));
  }
  if (query.status) {
    searchParams.set("status", query.status);
  }
  if (query.orderNo?.trim()) {
    searchParams.set("orderNo", query.orderNo.trim());
  }

  const search = searchParams.toString();
  return search ? `?${search}` : "";
}

async function safeJson(response: Response): Promise<unknown | null> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
