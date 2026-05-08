import Link from "next/link";

import { LogoutButton } from "@/components/logout-button";
import { getSessionUser } from "@/lib/auth/server";

export default async function HomePage() {
  const user = await getSessionUser();

  return (
    <main className="min-h-screen px-6 py-12">
      <section className="mx-auto flex max-w-5xl flex-col gap-6 rounded-[32px] border border-black/10 bg-[var(--surface)] p-10 shadow-[0_20px_60px_rgba(74,42,18,0.08)]">
        <span className="w-fit rounded-full bg-[var(--accent)] px-4 py-1 text-sm font-semibold text-white">
          hill-commerce MVP
        </span>
        <div className="flex flex-col gap-4">
          <h1 className="text-4xl font-bold tracking-tight">实体商品电商基础骨架已就绪</h1>
          <p className="max-w-3xl text-base leading-7 text-black/70">
            当前阶段完成了前后端、数据库配置、缓存与事件扩展位、以及基础部署骨架。接下来将按实施计划继续推进认证、商品、购物车、订单和后台管理。
          </p>
        </div>
        <div className="rounded-[24px] bg-white/80 p-6">
          {user ? (
            <div className="flex flex-col gap-4">
              <p className="text-base">
                当前登录用户：<span className="font-semibold">{user.nickname}</span>（{user.roles.join(", ")}）
              </p>
              <div className="flex flex-wrap gap-3">
                <Link className="rounded-full border border-black/10 px-5 py-2 font-medium" href="/account">
                  账户页
                </Link>
                <Link className="rounded-full border border-black/10 px-5 py-2 font-medium" href="/admin">
                  后台示例页
                </Link>
                <LogoutButton className="rounded-full bg-[var(--accent)] px-5 py-2 font-semibold text-white" />
              </div>
            </div>
          ) : (
            <div className="flex flex-wrap gap-3">
              <Link className="rounded-full bg-[var(--accent)] px-5 py-2 font-semibold text-white" href="/login">
                去登录
              </Link>
              <Link className="rounded-full border border-black/10 px-5 py-2 font-medium" href="/register">
                去注册
              </Link>
              <Link className="rounded-full border border-black/10 px-5 py-2 font-medium" href="/account">
                验证前台拦截
              </Link>
              <Link className="rounded-full border border-black/10 px-5 py-2 font-medium" href="/admin">
                验证后台拦截
              </Link>
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
