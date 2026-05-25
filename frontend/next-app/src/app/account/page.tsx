import Link from "next/link";

import { LogoutButton } from "@/components/logout-button";
import { requireUser } from "@/lib/auth/server";

import EditNicknameForm from "./edit-nickname-form";

export default async function AccountPage() {
  const user = await requireUser("/account");

  return (
    <main className="page-shell">
      <section className="mx-auto flex max-w-4xl flex-col bg-white">
        <span className="w-fit px-6 pt-6 text-sm text-[var(--text-secondary)]">我的账户</span>
        <div className="space-y-3 px-6 pb-6">
          <h1 className="text-3xl font-bold tracking-tight">你好，{user.nickname}</h1>
        </div>
        <div className="border-b border-gray-100" />
        <dl className="grid gap-4 px-6 py-4 md:grid-cols-2">
          <div>
            <dt className="text-sm text-[var(--text-secondary)]">邮箱</dt>
            <dd className="mt-1 font-semibold">{user.email}</dd>
          </div>
          <div>
            <dt className="text-sm text-[var(--text-secondary)]">昵称</dt>
            <dd className="mt-1">
              <EditNicknameForm currentNickname={user.nickname} />
            </dd>
          </div>
        </dl>
        <div className="border-b border-gray-100" />
        <div className="flex flex-wrap gap-3 px-6 py-4">
          <Link className="btn-secondary px-5 py-2" href="/">
            返回首页
          </Link>
          {(user.roles.includes("ADMIN") || user.roles.includes("MERCHANT")) ? (
            <Link className="btn-secondary px-5 py-2" href="/admin">
              我的后台
            </Link>
          ) : (
            <>
              <Link className="btn-secondary px-5 py-2" href="/orders">
                我的订单
              </Link>
              <Link className="btn-secondary px-5 py-2" href="/cart">
                购物车
              </Link>
            </>
          )}
          <LogoutButton className="btn-primary px-5 py-2" />
        </div>
      </section>
    </main>
  );
}
