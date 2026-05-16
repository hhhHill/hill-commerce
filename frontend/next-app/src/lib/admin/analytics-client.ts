"use client";

import type {
  AggregateProfileResponse,
  AnomalyItem,
  AnomalyStatusResponse,
  ProductRankingResponse,
  TrendResponse,
  UserProfileDetail,
  UserProfileSummary
} from "@/lib/admin/analytics-types";

export function getAnalyticsTrends(params: { granularity?: string; from?: string; to?: string } = {}) {
  const search = new URLSearchParams();
  if (params.granularity) search.set("granularity", params.granularity);
  if (params.from) search.set("from", params.from);
  if (params.to) search.set("to", params.to);
  return sendAnalyticsRequest<TrendResponse>(`/api/admin/analytics/trends${query(search)}`);
}

export function getAnomalyStatus() {
  return sendAnalyticsRequest<AnomalyStatusResponse>("/api/admin/analytics/anomalies/status");
}

export function getAnomalies() {
  return sendAnalyticsRequest<AnomalyItem[]>("/api/admin/analytics/anomalies");
}

export function acknowledgeAnomaly(id: string) {
  return sendAnalyticsRequest<AnomalyStatusResponse>(`/api/admin/analytics/anomalies/${encodeURIComponent(id)}/acknowledge`, {
    method: "POST"
  });
}

export function getProductRankings(params: { range?: string; limit?: number } = {}) {
  const search = new URLSearchParams();
  if (params.range) search.set("range", params.range);
  if (params.limit) search.set("limit", String(params.limit));
  return sendAnalyticsRequest<ProductRankingResponse>(`/api/admin/analytics/rankings/products${query(search)}`);
}

export function getAggregateProfiles() {
  return sendAnalyticsRequest<AggregateProfileResponse>("/api/admin/analytics/profiles/aggregate");
}

export function searchUsers(keyword: string) {
  const search = new URLSearchParams({ keyword });
  return sendAnalyticsRequest<UserProfileSummary[]>(`/api/admin/analytics/profiles/users/search?${search}`);
}

export function getUserProfile(userId: number) {
  return sendAnalyticsRequest<UserProfileDetail>(`/api/admin/analytics/profiles/users/${userId}`);
}

async function sendAnalyticsRequest<T>(input: RequestInfo, init: RequestInit = {}): Promise<T> {
  const response = await fetch(input, {
    ...init,
    credentials: "include",
    headers: {
      "content-type": "application/json",
      ...(init.headers ?? {})
    }
  });
  if (!response.ok) {
    throw new Error("分析数据请求失败");
  }
  return (await response.json()) as T;
}

function query(search: URLSearchParams) {
  const value = search.toString();
  return value ? `?${value}` : "";
}
