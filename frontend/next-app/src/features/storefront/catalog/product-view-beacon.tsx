"use client";

import { useEffect } from "react";

import { recordBrowseEvent, STOREFRONT_BROWSE_EVENTS } from "@/lib/storefront/logging";

type ProductViewBeaconProps = {
  categoryId: number;
  productId: number;
};

export function ProductViewBeacon({ categoryId, productId }: ProductViewBeaconProps) {
  useEffect(() => {
    recordBrowseEvent(STOREFRONT_BROWSE_EVENTS.productView, {
      categoryId,
      productId,
      source: "detail-page"
    });
  }, [categoryId, productId]);

  return null;
}
