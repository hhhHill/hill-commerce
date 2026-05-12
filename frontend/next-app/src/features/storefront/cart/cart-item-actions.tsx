"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, useTransition } from "react";

import { deleteCartItem, updateCartItem } from "@/lib/cart/client";
import type { CartItem } from "@/lib/cart/types";

type CartItemActionsProps = {
  item: CartItem;
};

export function CartItemActions({ item }: CartItemActionsProps) {
  const router = useRouter();
  const [quantity, setQuantity] = useState(item.quantity);
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    setQuantity(item.quantity);
  }, [item.quantity]);

  const commitUpdate = (nextQuantity: number, nextSelected: boolean) => {
    setError("");
    startTransition(async () => {
      try {
        await updateCartItem(item.id, {
          quantity: nextQuantity,
          selected: nextSelected
        });
        router.refresh();
      } catch (updateError) {
        setQuantity(item.quantity);
        setError(updateError instanceof Error ? updateError.message : "更新购物车失败");
      }
    });
  };

  return (
    <div className="flex flex-col gap-3">
      <label className="flex items-center gap-2 text-sm text-black/65">
        <input
          checked={item.selected}
          className="h-4 w-4 rounded border-black/20"
          disabled={isPending}
          type="checkbox"
          onChange={(event) => commitUpdate(quantity, event.target.checked)}
        />
        参与本次结算
      </label>

      <div className="flex items-center gap-3">
        <div className="flex items-center rounded-full border border-black/10 bg-white">
          <button
            className="px-3 py-2 text-sm font-semibold disabled:opacity-40"
            disabled={isPending || quantity <= 1}
            type="button"
            onClick={() => {
              const nextQuantity = Math.max(1, quantity - 1);
              setQuantity(nextQuantity);
              commitUpdate(nextQuantity, item.selected);
            }}
          >
            -
          </button>
          <span className="min-w-10 text-center text-sm font-semibold">{quantity}</span>
          <button
            className="px-3 py-2 text-sm font-semibold disabled:opacity-40"
            disabled={isPending}
            type="button"
            onClick={() => {
              const nextQuantity = quantity + 1;
              setQuantity(nextQuantity);
              commitUpdate(nextQuantity, item.selected);
            }}
          >
            +
          </button>
        </div>

        <button
          className="rounded-full border border-red-200 px-4 py-2 text-sm font-medium text-red-700 disabled:opacity-40"
          disabled={isPending}
          type="button"
          onClick={() => {
            setError("");
            startTransition(async () => {
              try {
                await deleteCartItem(item.id);
                router.refresh();
              } catch (deleteError) {
                setError(deleteError instanceof Error ? deleteError.message : "删除购物车项失败");
              }
            });
          }}
        >
          删除
        </button>
      </div>

      {error ? <p className="text-sm text-red-700">{error}</p> : null}
    </div>
  );
}
