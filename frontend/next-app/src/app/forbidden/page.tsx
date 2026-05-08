import Link from "next/link";

export default function ForbiddenPage() {
  return (
    <main className="min-h-screen px-6 py-12">
      <section className="mx-auto flex max-w-3xl flex-col gap-6 rounded-[32px] border border-[#8e2d1c]/10 bg-[var(--surface)] p-10 shadow-[0_20px_60px_rgba(74,42,18,0.08)]">
        <span className="w-fit rounded-full bg-[#8e2d1c] px-4 py-1 text-sm font-semibold text-white">403 Forbidden</span>
        <div className="space-y-3">
          <h1 className="text-4xl font-bold tracking-tight">你没有访问这个页面的权限</h1>
          <p className="text-base leading-7 text-black/70">
            当前账号已经登录，但角色边界不满足后台页面访问条件。你可以返回首页，或者切换成具备权限的账号。
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <Link className="rounded-full bg-[var(--accent)] px-5 py-2 font-semibold text-white" href="/">
            返回首页
          </Link>
          <Link className="rounded-full border border-black/10 px-5 py-2 font-medium" href="/account">
            前往账户页
          </Link>
        </div>
      </section>
    </main>
  );
}
