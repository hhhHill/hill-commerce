"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState, useTransition } from "react";

import { addCartItem } from "@/lib/cart/client";
import type { StorefrontProductDetail } from "@/lib/storefront/types";

type AddToCartPanelProps = {
  product: StorefrontProductDetail;
  isAuthenticated: boolean;
  loginHref: string;
};

export function AddToCartPanel({ product, isAuthenticated, loginHref }: AddToCartPanelProps) {
  const router = useRouter();
  const [selectedSkuId, setSelectedSkuId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isPending, startTransition] = useTransition();

  const availableSkus = useMemo(
    () => product.skus.filter((sku) => sku.status === "ENABLED" && sku.stockStatus !== "OUT_OF_STOCK"),
    [product.skus]
  );

  useEffect(() => {
    setSelectedSkuId(availableSkus[0]?.id ?? product.skus[0]?.id ?? null);
  }, [availableSkus, product.skus]);

  const selectedSku = product.skus.find((sku) => sku.id === selectedSkuId) ?? null;
  const cartAvailable = product.saleStatus === "AVAILABLE" && isAuthenticated && selectedSku && selectedSku.status === "ENABLED";

  return (
    <section className="rounded-[26px] border border-black/10 bg-[var(--surface)] px-4 py-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold">加入购物车</h2>
          <p className="mt-2 text-sm leading-7 text-black/60">购物车只对登录用户开放。当前只允许加入可售且有库存的 SKU。</p>
        </div>
        {selectedSku ? <p className="text-lg font-semibold text-[var(--accent-strong)]">¥{selectedSku.price}</p> : null}
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {product.skus.map((sku) => {
          const disabled = sku.status !== "ENABLED" || sku.stockStatus === "OUT_OF_STOCK";
          const active = selectedSkuId === sku.id;
          return (
            <button
              key={sku.id}
              className={`rounded-full border px-3 py-2 text-sm ${
                active ? "border-[var(--accent)] bg-white font-semibold text-[var(--accent-strong)]" : "border-black/10 bg-white text-black/70"
              } ${disabled ? "cursor-not-allowed opacity-45" : ""}`}
              disabled={disabled}
              type="button"
              onClick={() => setSelectedSkuId(sku.id)}
            >
              {sku.salesAttrValueText}
            </button>
          );
        })}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-3">
        <div className="flex items-center rounded-full border border-black/10 bg-white">
          <button
            className="px-3 py-2 text-sm font-semibold disabled:opacity-40"
            disabled={quantity <= 1 || isPending}
            type="button"
            onClick={() => setQuantity((current) => Math.max(1, current - 1))}
          >
            -
          </button>
          <span className="min-w-10 text-center text-sm font-semibold">{quantity}</span>
          <button
            className="px-3 py-2 text-sm font-semibold disabled:opacity-40"
            disabled={isPending}
            type="button"
            onClick={() => setQuantity((current) => current + 1)}
          >
            +
          </button>
        </div>

        {isAuthenticated ? (
          <button
            className={`rounded-full px-5 py-3 text-sm font-semibold text-white ${cartAvailable ? "bg-[var(--accent)]" : "bg-black/30"}`}
            disabled={!cartAvailable || isPending || !selectedSkuId}
            type="button"
            onClick={() => {
              if (!selectedSkuId) {
                return;
              }
              setError("");
              setSuccess("");
              startTransition(async () => {
                try {
                  await addCartItem({
                    skuId: selectedSkuId,
                    quantity
                  });
                  setSuccess("已加入购物车");
                  router.refresh();
                } catch (cartError) {
                  setError(cartError instanceof Error ? cartError.message : "加入购物车失败");
                }
              });
            }}
          >
            {isPending ? "加入中..." : "加入购物车"}
          </button>
        ) : (
          <Link className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white" href={loginHref}>
            登录后加购
          </Link>
        )}
      </div>

      {selectedSku ? (
        <p className="mt-3 text-sm text-black/55">
          当前选择：{selectedSku.salesAttrValueText} · {renderStockStatus(selectedSku.stockStatus, selectedSku.stock)}
        </p>
      ) : null}
      {error ? <p className="mt-3 text-sm text-red-700">{error}</p> : null}
      {success ? (
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <p className="text-sm text-emerald-700">{success}</p>
          <Link className="rounded-full border border-emerald-200 px-3 py-1 text-sm font-medium text-emerald-700" href="/cart">
            去购物车
          </Link>
        </div>
      ) : null}
    </section>
  );
}

function renderStockStatus(status: StorefrontProductDetail["skus"][number]["stockStatus"], stock: number): string {
  switch (status) {
    case "IN_STOCK":
      return `库存充足 · ${stock}`;
    case "LOW_STOCK":
      return `库存偏低 · ${stock}`;
    case "OUT_OF_STOCK":
      return "暂时缺货";
    default:
      return String(stock);
  }
}
