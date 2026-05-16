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
          className="rounded-lg border border-[var(--border-normal)] bg-white p-6 shadow-[0_2px_8px_rgba(0,0,0,0.04)] transition hover:-translate-y-0.5"
          href="/admin/categories"
        >
          <p className="text-sm uppercase tracking-[0.18em] text-[var(--text-secondary)]">Category</p>
          <h2 className="mt-3 text-2xl font-semibold">分类管理</h2>
          <p className="mt-3 text-sm leading-6 text-[var(--text-secondary)]">维护一级分类、排序和启停状态，为商品管理提供稳定的分类基础。</p>
        </Link>
        <Link
          className="rounded-lg border border-[var(--border-normal)] bg-white p-6 shadow-[0_2px_8px_rgba(0,0,0,0.04)] transition hover:-translate-y-0.5"
          href="/admin/products"
        >
          <p className="text-sm uppercase tracking-[0.18em] text-[var(--text-secondary)]">Product</p>
          <h2 className="mt-3 text-2xl font-semibold">商品管理</h2>
          <p className="mt-3 text-sm leading-6 text-[var(--text-secondary)]">
            通过一个聚合编辑页维护 SPU、展示属性、销售属性、图片 URL、描述和 SKU。
          </p>
        </Link>
        <Link
          className="rounded-lg border border-[var(--border-normal)] bg-white p-6 shadow-[0_2px_8px_rgba(0,0,0,0.04)] transition hover:-translate-y-0.5"
          href="/admin/orders"
        >
          <p className="text-sm uppercase tracking-[0.18em] text-[var(--text-secondary)]">Order</p>
          <h2 className="mt-3 text-2xl font-semibold">订单管理</h2>
          <p className="mt-3 text-sm leading-6 text-[var(--text-secondary)]">查看后台订单、进入发货表单，并手动触发自动完成流程。</p>
        </Link>
        <Link
          className="rounded-lg border border-[var(--border-normal)] bg-white p-6 shadow-[0_2px_8px_rgba(0,0,0,0.04)] transition hover:-translate-y-0.5"
          href="/admin/logs"
        >
          <p className="text-sm uppercase tracking-[0.18em] text-[var(--text-secondary)]">Observability</p>
          <h2 className="mt-3 text-2xl font-semibold">日志中心</h2>
          <p className="mt-3 text-sm leading-6 text-[var(--text-secondary)]">集中查询登录日志、操作日志和商品浏览日志，便于运营排查与审计。</p>
        </Link>
      </section>
    </AdminShell>
  );
}
