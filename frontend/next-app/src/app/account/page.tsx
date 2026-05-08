import Link from "next/link";

import { LogoutButton } from "@/components/logout-button";
import { requireUser } from "@/lib/auth/server";

export default async function AccountPage() {
  const user = await requireUser("/account");

  return (
    <main className="min-h-screen px-6 py-12">
      <section className="mx-auto flex max-w-4xl flex-col gap-6 rounded-[32px] border border-black/10 bg-[var(--surface)] p-10 shadow-[0_20px_60px_rgba(74,42,18,0.08)]">
        <span className="w-fit rounded-full bg-[var(--accent)] px-4 py-1 text-sm font-semibold text-white">
          Protected Account
        </span>
        <div className="space-y-3">
          <h1 className="text-4xl font-bold tracking-tight">你好，{user.nickname}</h1>
          <p className="text-base leading-7 text-black/70">
            当前页面用于承接任务三的前台受保护路由验收。它只在服务端确认会话有效后渲染。
          </p>
        </div>
        <dl className="grid gap-4 rounded-[24px] bg-white/80 p-6 md:grid-cols-2">
          <div>
            <dt className="text-sm text-black/55">邮箱</dt>
            <dd className="mt-1 font-semibold">{user.email}</dd>
          </div>
          <div>
            <dt className="text-sm text-black/55">角色</dt>
            <dd className="mt-1 font-semibold">{user.roles.join(", ")}</dd>
          </div>
        </dl>
        <div className="flex flex-wrap gap-3">
          <Link className="rounded-full border border-black/10 px-5 py-2 font-medium" href="/">
            返回首页
          </Link>
          <Link className="rounded-full border border-black/10 px-5 py-2 font-medium" href="/admin">
            访问后台示例页
          </Link>
          <LogoutButton className="rounded-full bg-[var(--accent)] px-5 py-2 font-semibold text-white" />
        </div>
      </section>
    </main>
  );
}
