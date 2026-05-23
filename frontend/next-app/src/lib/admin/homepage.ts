"use client";

import type { ApiErrorResponse } from "@/lib/admin/types";

export type ActivityCard = {
  id: number;
  placement: string;
  position: number;
  title: string;
  imageUrl: string | null;
  linkUrl: string;
  isActive: boolean;
  sortOrder: number;
};

export type ActivityCardList = {
  items: ActivityCard[];
};

export type BatchUpdateActivityCardsInput = {
  cards: {
    id: number;
    title: string;
    imageUrl: string | null;
    linkUrl: string;
    isActive: boolean;
    sortOrder: number;
  }[];
};

export async function listActivityCards(placement = "homepage"): Promise<ActivityCard[]> {
  const result = await sendAdminRequest<ActivityCardList>(`/api/admin/activity-cards?placement=${encodeURIComponent(placement)}`, {
    method: "GET"
  });
  return result.items;
}

export async function batchUpdateActivityCards(input: BatchUpdateActivityCardsInput): Promise<ActivityCard[]> {
  const result = await sendAdminRequest<ActivityCardList>("/api/admin/activity-cards", {
    method: "PUT",
    body: JSON.stringify(input)
  });
  return result.items;
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
