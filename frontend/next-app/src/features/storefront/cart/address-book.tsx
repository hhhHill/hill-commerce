"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState, useTransition } from "react";

import { AddressForm } from "@/features/storefront/cart/address-form";
import { CartEmptyState } from "@/features/storefront/cart/cart-empty-state";
import { deleteUserAddress, setDefaultUserAddress } from "@/lib/cart/client";
import type { UserAddress } from "@/lib/cart/types";

type AddressBookProps = {
  addresses: UserAddress[];
};

export function AddressBook({ addresses }: AddressBookProps) {
  const router = useRouter();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(addresses.length === 0);
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  const editingAddress = useMemo(
    () => addresses.find((address) => address.id === editingId) ?? null,
    [addresses, editingId]
  );

  return (
    <section className="flex flex-col gap-5">
      {addresses.length === 0 && !isCreateOpen ? (
        <CartEmptyState
          actionHref="/account/addresses"
          actionLabel="新增地址"
          description="当前还没有收货地址。新增首条地址后，系统会自动把它设成默认地址。"
          title="还没有收货地址"
        />
      ) : null}

      {isCreateOpen ? (
        <AddressForm
          onSaved={() => {
            setIsCreateOpen(false);
            router.refresh();
          }}
        />
      ) : (
        <button
          className="w-fit rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white"
          type="button"
          onClick={() => setIsCreateOpen(true)}
        >
          新增地址
        </button>
      )}

      {editingAddress ? (
        <AddressForm
          address={editingAddress}
          onCancel={() => setEditingId(null)}
          onSaved={() => {
            setEditingId(null);
            router.refresh();
          }}
        />
      ) : null}

      <div className="grid gap-4">
        {addresses.map((address) => (
          <article key={address.id} className="rounded-[26px] border border-black/10 bg-white/85 p-5 shadow-[0_18px_50px_rgba(74,42,18,0.06)]">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="space-y-2">
                <div className="flex flex-wrap items-center gap-3">
                  <h2 className="text-xl font-semibold">{address.receiverName}</h2>
                  <span className="text-sm text-black/55">{address.receiverPhone}</span>
                  {address.isDefault ? (
                    <span className="rounded-full bg-[var(--accent)] px-3 py-1 text-xs font-semibold text-white">默认地址</span>
                  ) : null}
                </div>
                <p className="text-sm leading-7 text-black/65">
                  {address.province}
                  {address.city}
                  {address.district}
                  {address.detailAddress}
                  {address.postalCode ? ` · ${address.postalCode}` : ""}
                </p>
              </div>

              <div className="flex flex-wrap gap-3">
                {!address.isDefault ? (
                  <button
                    className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium disabled:opacity-40"
                    disabled={isPending}
                    type="button"
                    onClick={() => {
                      setError("");
                      startTransition(async () => {
                        try {
                          await setDefaultUserAddress(address.id);
                          router.refresh();
                        } catch (setDefaultError) {
                          setError(setDefaultError instanceof Error ? setDefaultError.message : "设置默认地址失败");
                        }
                      });
                    }}
                  >
                    设为默认
                  </button>
                ) : null}
                <button
                  className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium"
                  type="button"
                  onClick={() => setEditingId(address.id)}
                >
                  编辑
                </button>
                <button
                  className="rounded-full border border-red-200 px-4 py-2 text-sm font-medium text-red-700 disabled:opacity-40"
                  disabled={isPending}
                  type="button"
                  onClick={() => {
                    setError("");
                    startTransition(async () => {
                      try {
                        await deleteUserAddress(address.id);
                        if (editingId === address.id) {
                          setEditingId(null);
                        }
                        router.refresh();
                      } catch (deleteError) {
                        setError(deleteError instanceof Error ? deleteError.message : "删除地址失败");
                      }
                    });
                  }}
                >
                  删除
                </button>
              </div>
            </div>
          </article>
        ))}
      </div>

      {error ? <p className="text-sm text-red-700">{error}</p> : null}
    </section>
  );
}
