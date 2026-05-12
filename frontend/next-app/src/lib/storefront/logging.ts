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
