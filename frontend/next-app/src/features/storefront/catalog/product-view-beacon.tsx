"use client";

import { useEffect } from "react";

import { reportViewLog } from "@/lib/storefront/logging";

type ProductViewBeaconProps = {
  categoryId: number;
  productId: number;
};

export function ProductViewBeacon({ categoryId, productId }: ProductViewBeaconProps) {
  useEffect(() => {
    void reportViewLog({ categoryId, productId });
  }, [categoryId, productId]);

  return null;
}
