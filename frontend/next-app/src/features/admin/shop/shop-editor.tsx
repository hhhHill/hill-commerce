"use client";

import { useEffect, useState, useTransition } from "react";

import { getMyShop, updateMyShop } from "@/lib/admin/client";
import type { Shop } from "@/lib/admin/types";

export function ShopEditor() {
  const [shop, setShop] = useState<Shop | null>(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    startTransition(async () => {
      try {
        const data = await getMyShop();
        setShop(data);
        setName(data.name ?? "");
        setDescription(data.description ?? "");
      } catch {
        setMessage("加载店铺信息失败");
      } finally {
        setLoading(false);
      }
    });
  }, []);

  function handleSave(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setMessage("");

    startTransition(async () => {
      try {
        const updated = await updateMyShop({ name, description });
        setShop(updated);
        setName(updated.name ?? "");
        setDescription(updated.description ?? "");
        setMessage("保存成功");
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "保存失败");
      }
    });
  }

  if (loading) {
    return (
      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <p className="text-sm text-black/65">加载中...</p>
      </section>
    );
  }

  return (
    <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <h2 className="text-2xl font-semibold">编辑店铺信息</h2>
      <p className="mt-2 text-sm text-black/65">修改后保存将立即生效。</p>

      <form className="mt-6 space-y-4" onSubmit={handleSave}>
        <label className="flex flex-col gap-2 text-sm font-medium">
          店铺名称
          <input
            required
            className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
            onChange={(e) => setName(e.target.value)}
            placeholder="请输入店铺名称"
            value={name}
          />
        </label>

        <label className="flex flex-col gap-2 text-sm font-medium">
          店铺简介
          <textarea
            className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
            onChange={(e) => setDescription(e.target.value)}
            placeholder="请输入店铺简介"
            rows={4}
            value={description}
          />
        </label>

        <div className="flex flex-wrap gap-3">
          <button
            className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white disabled:opacity-60"
            disabled={isPending}
            type="submit"
          >
            {isPending ? "保存中..." : "保存"}
          </button>
        </div>

        {message ? (
          <p
            className={
              message.includes("成功")
                ? "text-sm text-[#186a3b]"
                : "text-sm text-red-700"
            }
          >
            {message}
          </p>
        ) : null}
      </form>
    </section>
  );
}
