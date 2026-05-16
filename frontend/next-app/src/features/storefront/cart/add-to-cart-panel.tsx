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
  const cartAvailable =
    product.saleStatus === "AVAILABLE" &&
    isAuthenticated &&
    selectedSku &&
    selectedSku.status === "ENABLED" &&
    selectedSku.stockStatus !== "OUT_OF_STOCK";

  return (
    <section className="surface-subtle px-4 py-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold">选择规格后立即购买</h2>
          <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">当前只允许加入可售且有库存的 SKU，马上抢会先加购，再跳到购物车。</p>
        </div>
        {selectedSku ? (
          <p className="text-2xl font-bold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
            ¥{selectedSku.price}
          </p>
        ) : null}
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {product.skus.map((sku) => {
          const disabled = sku.status !== "ENABLED" || sku.stockStatus === "OUT_OF_STOCK";
          const active = selectedSkuId === sku.id;
          return (
            <button
              key={sku.id}
              className={`min-h-11 rounded-2xl border px-3 py-2 text-sm ${
                active
                  ? "border-[var(--brand-primary)] bg-[#fff2ec] font-semibold text-[var(--brand-primary)]"
                  : "border-[var(--border-normal)] bg-white text-[var(--text-secondary)]"
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

      <div className="mt-4 flex flex-col gap-3">
        <div className="flex items-center rounded-3xl border border-[var(--border-normal)] bg-white">
          <button
            className="min-h-11 px-4 text-sm font-semibold disabled:opacity-40"
            disabled={quantity <= 1 || isPending}
            type="button"
            onClick={() => setQuantity((current) => Math.max(1, current - 1))}
          >
            -
          </button>
          <span className="min-w-12 text-center text-sm font-semibold">{quantity}</span>
          <button
            className="min-h-11 px-4 text-sm font-semibold disabled:opacity-40"
            disabled={isPending}
            type="button"
            onClick={() => setQuantity((current) => current + 1)}
          >
            +
          </button>
        </div>

        {isAuthenticated ? (
          <div className="grid gap-3 sm:grid-cols-2">
            <button
              className={`btn-secondary w-full ${cartAvailable ? "" : "cursor-not-allowed opacity-45"}`}
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
            <button
              className={`btn-primary w-full ${cartAvailable ? "" : "btn-disabled"}`}
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
                    router.push("/cart");
                    router.refresh();
                  } catch (cartError) {
                    setError(cartError instanceof Error ? cartError.message : "立即购买失败");
                  }
                });
              }}
            >
              {isPending ? "处理中..." : "马上抢"}
            </button>
          </div>
        ) : (
          <Link className="btn-primary w-full" href={loginHref}>
            登录后加购
          </Link>
        )}
      </div>

      {selectedSku ? (
        <p className="mt-3 text-sm text-[var(--text-secondary)]">
          当前选择：{selectedSku.salesAttrValueText} · {renderStockStatus(selectedSku.stockStatus, selectedSku.stock)}
        </p>
      ) : null}
      {error ? <p className="mt-3 text-sm text-red-700">{error}</p> : null}
      {success ? (
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <p className="text-sm text-emerald-700">{success}</p>
          <Link className="btn-secondary px-3 py-1 text-emerald-700" href="/cart">
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
