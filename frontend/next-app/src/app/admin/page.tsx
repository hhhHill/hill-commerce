import Link from "next/link";

import { requireRole } from "@/lib/auth/server";

export default async function AdminPage() {
  const user = await requireRole(["ADMIN", "SALES"], "/admin");

  return (
    <main className="min-h-screen px-6 py-12">
      <section className="mx-auto flex max-w-4xl flex-col gap-6 rounded-[32px] border border-black/10 bg-[var(--surface)] p-10 shadow-[0_20px_60px_rgba(74,42,18,0.08)]">
        <span className="w-fit rounded-full bg-[var(--accent-strong)] px-4 py-1 text-sm font-semibold text-white">
          Admin Boundary
        </span>
        <div className="space-y-3">
          <h1 className="text-4xl font-bold tracking-tight">后台受保护页</h1>
          <p className="text-base leading-7 text-black/70">
            这个页面只允许 `ADMIN` 或 `SALES` 角色进入，用于任务三的后台权限边界验收。
          </p>
        </div>
        <dl className="grid gap-4 rounded-[24px] bg-white/80 p-6 md:grid-cols-2">
          <div>
            <dt className="text-sm text-black/55">当前用户</dt>
            <dd className="mt-1 font-semibold">{user.nickname}</dd>
          </div>
          <div>
            <dt className="text-sm text-black/55">角色</dt>
            <dd className="mt-1 font-semibold">{user.roles.join(", ")}</dd>
          </div>
        </dl>
        <Link className="w-fit rounded-full border border-black/10 px-5 py-2 font-medium" href="/">
          返回首页
        </Link>
      </section>
    </main>
  );
}
