"use client";

import { useState, useTransition } from "react";

import { createUserAddress, updateUserAddress } from "@/lib/cart/client";
import type { UserAddress, UserAddressInput } from "@/lib/cart/types";

type AddressFormProps = {
  address?: UserAddress;
  onCancel?: () => void;
  onSaved: () => void;
};

export function AddressForm({ address, onCancel, onSaved }: AddressFormProps) {
  const [form, setForm] = useState<UserAddressInput>(() => ({
    receiverName: address?.receiverName ?? "",
    receiverPhone: address?.receiverPhone ?? "",
    province: address?.province ?? "",
    city: address?.city ?? "",
    district: address?.district ?? "",
    detailAddress: address?.detailAddress ?? "",
    postalCode: address?.postalCode ?? ""
  }));
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  const submitLabel = address ? "保存地址" : "新增地址";

  return (
    <form
      className="grid gap-4 rounded-[24px] border border-black/10 bg-[var(--surface)] p-4"
      onSubmit={(event) => {
        event.preventDefault();
        setError("");
        startTransition(async () => {
          try {
            if (address) {
              await updateUserAddress(address.id, form);
            } else {
              await createUserAddress(form);
            }
            onSaved();
          } catch (saveError) {
            setError(saveError instanceof Error ? saveError.message : "保存地址失败");
          }
        });
      }}
    >
      <div className="grid gap-4 md:grid-cols-2">
        <Field
          label="收货人"
          value={form.receiverName}
          onChange={(value) => setForm((current) => ({ ...current, receiverName: value }))}
        />
        <Field
          label="联系电话"
          value={form.receiverPhone}
          onChange={(value) => setForm((current) => ({ ...current, receiverPhone: value }))}
        />
        <Field label="省" value={form.province} onChange={(value) => setForm((current) => ({ ...current, province: value }))} />
        <Field label="市" value={form.city} onChange={(value) => setForm((current) => ({ ...current, city: value }))} />
        <Field
          label="区 / 县"
          value={form.district}
          onChange={(value) => setForm((current) => ({ ...current, district: value }))}
        />
        <Field
          label="邮编"
          value={form.postalCode ?? ""}
          onChange={(value) => setForm((current) => ({ ...current, postalCode: value }))}
        />
      </div>

      <label className="flex flex-col gap-2">
        <span className="text-sm font-medium text-black/65">详细地址</span>
        <textarea
          className="min-h-24 rounded-[20px] border border-black/10 bg-white px-4 py-3 text-sm outline-none"
          value={form.detailAddress}
          onChange={(event) => setForm((current) => ({ ...current, detailAddress: event.target.value }))}
        />
      </label>

      <div className="flex flex-wrap gap-3">
        <button
          className="rounded-full bg-[var(--accent)] px-5 py-2 text-sm font-semibold text-white disabled:opacity-40"
          disabled={isPending}
          type="submit"
        >
          {isPending ? "保存中..." : submitLabel}
        </button>
        {onCancel ? (
          <button className="rounded-full border border-black/10 px-5 py-2 text-sm font-medium" type="button" onClick={onCancel}>
            取消
          </button>
        ) : null}
      </div>

      {error ? <p className="text-sm text-red-700">{error}</p> : null}
    </form>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-sm font-medium text-black/65">{label}</span>
      <input
        className="rounded-full border border-black/10 bg-white px-4 py-3 text-sm outline-none"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}
