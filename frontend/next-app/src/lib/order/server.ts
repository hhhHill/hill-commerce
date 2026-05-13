import { cookies } from "next/headers";

import { getBackendBaseUrl } from "@/lib/config";
import { OrderRequestError } from "@/lib/order/errors";
import type { OrderCheckout, OrderDetail } from "@/lib/order/types";

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

async function getCookieHeader(): Promise<string> {
  const cookieStore = await cookies();
  return cookieStore
    .getAll()
    .map(({ name, value }) => `${name}=${value}`)
    .join("; ");
}
