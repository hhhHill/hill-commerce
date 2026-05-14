import { cookies } from "next/headers";

import { getBackendBaseUrl } from "@/lib/config";
import { OrderRequestError } from "@/lib/order/errors";
import type { OrderCheckout, OrderDetail, OrderListQuery, OrderListResult } from "@/lib/order/types";

export async function getServerOrders(query?: OrderListQuery): Promise<OrderListResult> {
  return fetchOrderJson<OrderListResult>(`/api/orders${buildOrderListSearch(query)}`);
}

export async function getServerOrderCheckout(): Promise<OrderCheckout> {
  return fetchOrderJson<OrderCheckout>("/api/orders/checkout");
}

export async function getServerOrder(orderId: number): Promise<OrderDetail> {
  return fetchOrderJson<OrderDetail>(`/api/orders/${orderId}`);
}

async function fetchOrderJson<T>(pathname: string): Promise<T> {
  const response = await fetch(`${getBackendBaseUrl()}${pathname}`, {
    method: "GET",
    headers: {
      cookie: await getCookieHeader()
    },
    cache: "no-store",
    redirect: "manual"
  });

  if (!response.ok) {
    throw new OrderRequestError(response.status, `Failed to load order data: ${response.status}`);
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

async function getCookieHeader(): Promise<string> {
  const cookieStore = await cookies();
  return cookieStore
    .getAll()
    .map(({ name, value }) => `${name}=${value}`)
    .join("; ");
}
