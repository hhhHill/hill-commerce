"use client";

import { CartRequestError } from "@/lib/cart/errors";
import type {
  AddCartItemInput,
  ApiErrorResponse,
  Cart,
  CartMutation,
  CheckoutSummary,
  UpdateCartItemInput,
  UserAddress,
  UserAddressInput
} from "@/lib/cart/types";

export async function getCart(): Promise<Cart> {
  return sendCartRequest<Cart>("/api/cart");
}

export async function addCartItem(input: AddCartItemInput): Promise<CartMutation> {
  return sendCartRequest<CartMutation>("/api/cart", {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function updateCartItem(itemId: number, input: UpdateCartItemInput): Promise<CartMutation> {
  return sendCartRequest<CartMutation>(`/api/cart/${itemId}`, {
    method: "PUT",
    body: JSON.stringify(input)
  });
}

export async function deleteCartItem(itemId: number): Promise<void> {
  await sendCartRequest(`/api/cart/${itemId}`, {
    method: "DELETE"
  });
}

export async function getCheckoutSummary(): Promise<CheckoutSummary> {
  return sendCartRequest<CheckoutSummary>("/api/cart/summary");
}

export async function getUserAddresses(): Promise<UserAddress[]> {
  return sendCartRequest<UserAddress[]>("/api/user/addresses");
}

export async function createUserAddress(input: UserAddressInput): Promise<UserAddress> {
  return sendCartRequest<UserAddress>("/api/user/addresses", {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function updateUserAddress(addressId: number, input: UserAddressInput): Promise<UserAddress> {
  return sendCartRequest<UserAddress>(`/api/user/addresses/${addressId}`, {
    method: "PUT",
    body: JSON.stringify(input)
  });
}

export async function deleteUserAddress(addressId: number): Promise<void> {
  await sendCartRequest(`/api/user/addresses/${addressId}`, {
    method: "DELETE"
  });
}

export async function setDefaultUserAddress(addressId: number): Promise<UserAddress> {
  return sendCartRequest<UserAddress>(`/api/user/addresses/${addressId}/default`, {
    method: "PUT"
  });
}

async function sendCartRequest<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
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
    throw new CartRequestError(response.status, payload?.message ?? "请求失败，请稍后重试");
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
