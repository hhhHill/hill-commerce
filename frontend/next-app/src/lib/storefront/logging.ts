"use client";

export const STOREFRONT_BROWSE_EVENTS = {
  categoryEnter: "storefront.category.enter",
  productClick: "storefront.product.click",
  productView: "storefront.product.view",
  searchSubmit: "storefront.search.submit"
} as const;

type BrowseEventName = (typeof STOREFRONT_BROWSE_EVENTS)[keyof typeof STOREFRONT_BROWSE_EVENTS];

export type BrowseEventPayload = {
  categoryId?: number | string;
  productId?: number | string;
  keyword?: string;
  source?: string;
};

type ReportViewLogPayload = {
  categoryId: number;
  productId: number;
};

const ANONYMOUS_ID_STORAGE_KEY = "hill-commerce:anonymous-id";

export function recordBrowseEvent(name: BrowseEventName, payload: BrowseEventPayload = {}) {
  if (typeof window === "undefined") {
    return;
  }

  window.dispatchEvent(
    new CustomEvent("hill-commerce:storefront-browse", {
      detail: {
        name,
        payload,
        occurredAt: new Date().toISOString()
      }
    })
  );
}

export function getOrCreateAnonymousId(): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const existing = window.localStorage.getItem(ANONYMOUS_ID_STORAGE_KEY);
    if (existing) {
      return existing;
    }

    const generated = typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
      ? crypto.randomUUID()
      : `anon-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    window.localStorage.setItem(ANONYMOUS_ID_STORAGE_KEY, generated);
    return generated;
  } catch {
    return null;
  }
}

export async function reportViewLog(payload: ReportViewLogPayload): Promise<void> {
  const anonymousId = getOrCreateAnonymousId();
  if (!anonymousId) {
    return;
  }

  try {
    await fetch("/api/storefront/view-log", {
      method: "POST",
      credentials: "include",
      headers: {
        "content-type": "application/json"
      },
      body: JSON.stringify({
        productId: payload.productId,
        categoryId: payload.categoryId,
        anonymousId
      }),
      keepalive: true
    });
  } catch {
    // Fire-and-forget reporting should never block the detail page.
  }
}
