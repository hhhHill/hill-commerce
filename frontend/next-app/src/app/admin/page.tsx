import Link from "next/link";

import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { requireRole } from "@/lib/auth/server";

export default async function AdminPage() {
  const user = await requireRole(["ADMIN", "SALES"], "/admin");

  return (
    <AdminShell
      description="这里汇总 task 4 的后台商品管理入口。Sales 和 Admin 可以从这里进入分类与商品管理。"
      title="后台总览"
      user={user}
    >
      <section className="grid gap-6 lg:grid-cols-2">
        <Link
          className="rounded-[28px] border border-black/10 bg-white/90 p-8 shadow-[0_16px_40px_rgba(29,20,13,0.06)] transition hover:-translate-y-0.5"
          href="/admin/categories"
        >
          <p className="text-sm uppercase tracking-[0.18em] text-black/45">Category</p>
          <h2 className="mt-3 text-2xl font-semibold">分类管理</h2>
          <p className="mt-3 text-sm leading-6 text-black/65">维护一级分类、排序和启停状态，为商品管理提供稳定的分类基础。</p>
        </Link>
        <Link
          className="rounded-[28px] border border-black/10 bg-white/90 p-8 shadow-[0_16px_40px_rgba(29,20,13,0.06)] transition hover:-translate-y-0.5"
          href="/admin/products"
        >
          <p className="text-sm uppercase tracking-[0.18em] text-black/45">Product</p>
          <h2 className="mt-3 text-2xl font-semibold">商品管理</h2>
          <p className="mt-3 text-sm leading-6 text-black/65">
            通过一个聚合编辑页维护 SPU、展示属性、销售属性、图片 URL、描述和 SKU。
          </p>
        </Link>
      </section>
    </AdminShell>
  );
}
