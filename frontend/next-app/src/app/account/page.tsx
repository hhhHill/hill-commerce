import Link from "next/link";

import { LogoutButton } from "@/components/logout-button";
import { requireUser } from "@/lib/auth/server";

export default async function AccountPage() {
  const user = await requireUser("/account");

  return (
    <main className="page-shell">
      <section className="surface-card mx-auto flex max-w-4xl flex-col gap-5 rounded-lg p-6">
        <span className="chip-badge w-fit">我的账户</span>
        <div className="space-y-3">
          <h1 className="text-3xl font-bold tracking-tight">你好，{user.nickname}</h1>
          <p className="text-base leading-7 text-[var(--text-secondary)]">
            当前页面用于承接任务三的前台受保护路由验收。它只在服务端确认会话有效后渲染。
          </p>
        </div>
        <dl className="surface-subtle grid gap-4 p-4 md:grid-cols-2">
          <div>
            <dt className="text-sm text-[var(--text-secondary)]">邮箱</dt>
            <dd className="mt-1 font-semibold">{user.email}</dd>
          </div>
          <div>
            <dt className="text-sm text-[var(--text-secondary)]">角色</dt>
            <dd className="mt-1 font-semibold">{user.roles.join(", ")}</dd>
          </div>
        </dl>
        <div className="flex flex-wrap gap-3">
          <Link className="btn-secondary px-5 py-2" href="/">
            返回首页
          </Link>
          <Link className="btn-secondary px-5 py-2" href="/orders">
            我的订单
          </Link>
          <Link className="btn-secondary px-5 py-2" href="/cart">
            购物车
          </Link>
          <Link className="btn-secondary px-5 py-2" href="/admin">
            访问后台示例页
          </Link>
          <LogoutButton className="btn-primary px-5 py-2" />
        </div>
      </section>
    </main>
  );
}
