import { cookies } from "next/headers";

import { getBackendBaseUrl } from "@/lib/config";
import { PaymentRequestError } from "@/lib/payment/errors";
import type { PaymentOrder } from "@/lib/payment/types";

export async function getServerPaymentOrder(orderId: number): Promise<PaymentOrder> {
  return fetchPaymentJson<PaymentOrder>(`/api/payments/orders/${orderId}`);
}

async function fetchPaymentJson<T>(pathname: string): Promise<T> {
  const response = await fetch(`${getBackendBaseUrl()}${pathname}`, {
    method: "GET",
    headers: {
      cookie: await getCookieHeader()
    },
    cache: "no-store",
    redirect: "manual"
  });

  if (!response.ok) {
    throw new PaymentRequestError(response.status, `Failed to load payment data: ${response.status}`);
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
