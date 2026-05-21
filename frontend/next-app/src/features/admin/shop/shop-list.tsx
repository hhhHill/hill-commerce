"use client";

import { useEffect, useState, useTransition } from "react";

import { disableShop, enableShop, listShops } from "@/lib/admin/client";
import type { Shop } from "@/lib/admin/types";

export function ShopList() {
  const [shops, setShops] = useState<Shop[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    loadShops();
  }, []);

  async function loadShops() {
    try {
      const result = await listShops(1, 50);
      setShops(result.items ?? []);
    } catch {
      setMessage("加载店铺列表失败");
    } finally {
      setLoading(false);
    }
  }

  function handleToggle(shop: Shop) {
    const action = shop.status === "ACTIVE" ? "停用" : "启用";
    if (!window.confirm(`确认${action}店铺「${shop.name}」吗？`)) {
      return;
    }

    setMessage("");
    startTransition(async () => {
      try {
        if (shop.status === "ACTIVE") {
          await disableShop(shop.id);
          setMessage(`店铺「${shop.name}」已停用`);
        } else {
          await enableShop(shop.id);
          setMessage(`店铺「${shop.name}」已启用`);
        }
        await loadShops();
      } catch (error) {
        setMessage(error instanceof Error ? error.message : `${action}店铺失败`);
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
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold">全部店铺</h2>
          <p className="mt-2 text-sm text-black/65">查看和管理所有店铺，支持停用和重新启用。</p>
        </div>
      </div>

      {message ? <p className="mt-4 text-sm text-black/65">{message}</p> : null}

      <div className="mt-6">
        {shops.length === 0 ? (
          <p className="rounded-[24px] border border-dashed border-black/10 px-5 py-10 text-center text-sm text-black/55">
            当前还没有店铺。
          </p>
        ) : (
          <div className="space-y-4">
            {shops.map((shop) => (
              <article
                key={shop.id}
                className="rounded-[24px] border border-black/10 bg-[#fffaf5] p-5 shadow-[0_10px_26px_rgba(29,20,13,0.04)]"
              >
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div className="space-y-2">
                    <div className="flex flex-wrap items-center gap-3">
                      <span
                        className={
                          shop.status === "ACTIVE"
                            ? "rounded-full bg-[#ddf4df] px-3 py-1 text-sm font-medium text-[#186a3b]"
                            : "rounded-full bg-[#f7d9d9] px-3 py-1 text-sm font-medium text-[#8a1c1c]"
                        }
                      >
                        {shop.status === "ACTIVE" ? "营业中" : "已停用"}
                      </span>
                      <span className="text-sm text-black/50">{shop.slug}</span>
                    </div>
                    <h3 className="text-xl font-semibold">{shop.name}</h3>
                    {shop.description ? (
                      <p className="text-sm text-black/65 line-clamp-2">{shop.description}</p>
                    ) : null}
                  </div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      className={
                        shop.status === "ACTIVE"
                          ? "rounded-full bg-[#8a1c1c] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
                          : "rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
                      }
                      disabled={isPending}
                      type="button"
                      onClick={() => handleToggle(shop)}
                    >
                      {shop.status === "ACTIVE" ? "停用" : "启用"}
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
