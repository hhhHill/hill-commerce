"use client";

import { PaymentRequestError } from "@/lib/payment/errors";
import type {
  CloseExpiredPaymentsResult,
  PaymentActionResult,
  PaymentApiErrorResponse,
  PaymentAttempt,
  PaymentOrder
} from "@/lib/payment/types";

export async function getPaymentOrder(orderId: number): Promise<PaymentOrder> {
  return sendPaymentRequest<PaymentOrder>(`/api/payments/orders/${orderId}`);
}

export async function createPaymentAttempt(orderId: number): Promise<PaymentAttempt> {
  return sendPaymentRequest<PaymentAttempt>(`/api/payments/orders/${orderId}/attempts`, {
    method: "POST"
  });
}

export async function succeedPayment(paymentId: number): Promise<PaymentActionResult> {
  return sendPaymentRequest<PaymentActionResult>(`/api/payments/${paymentId}/succeed`, {
    method: "POST"
  });
}

export async function failPayment(paymentId: number): Promise<PaymentActionResult> {
  return sendPaymentRequest<PaymentActionResult>(`/api/payments/${paymentId}/fail`, {
    method: "POST"
  });
}

export async function closeExpiredPayments(): Promise<CloseExpiredPaymentsResult> {
  return sendPaymentRequest<CloseExpiredPaymentsResult>("/api/payments/close-expired", {
    method: "POST"
  });
}

async function sendPaymentRequest<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    credentials: "include",
    headers: {
      "content-type": "application/json",
      ...(init?.headers ?? {})
    }
  });

  if (!response.ok) {
    const payload = (await safeJson(response)) as PaymentApiErrorResponse | null;
    throw new PaymentRequestError(response.status, payload?.message ?? "请求失败，请稍后重试");
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
