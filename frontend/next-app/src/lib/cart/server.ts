import { cookies } from "next/headers";

import { getBackendBaseUrl } from "@/lib/config";
import { CartRequestError } from "@/lib/cart/errors";
import type { Cart, CheckoutSummary, UserAddress } from "@/lib/cart/types";

export async function getServerCart(): Promise<Cart> {
  return fetchCartJson<Cart>("/api/cart");
}

export async function getServerCheckoutSummary(): Promise<CheckoutSummary> {
  return fetchCartJson<CheckoutSummary>("/api/cart/summary");
}

export async function getServerUserAddresses(): Promise<UserAddress[]> {
  return fetchCartJson<UserAddress[]>("/api/user/addresses");
}

async function fetchCartJson<T>(pathname: string): Promise<T> {
  const response = await fetch(`${getBackendBaseUrl()}${pathname}`, {
    method: "GET",
    headers: {
      cookie: await getCookieHeader()
    },
    cache: "no-store",
    redirect: "manual"
  });

  if (!response.ok) {
    throw new CartRequestError(response.status, `Failed to load cart data: ${response.status}`);
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
