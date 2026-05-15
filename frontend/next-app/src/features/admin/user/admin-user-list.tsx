"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { disableSalesUser, enableSalesUser, resetSalesPassword } from "@/lib/admin/client";
import type { SalesUser } from "@/lib/admin/types";

type AdminUserListProps = {
  users: SalesUser[];
};

export function AdminUserList({ users }: AdminUserListProps) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  function handleResetPassword(user: SalesUser) {
    const password = window.prompt(`为 ${user.nickname} 设置新密码（至少 6 位）`, "");
    if (!password) {
      return;
    }

    setMessage("");
    startTransition(async () => {
      try {
        await resetSalesPassword(user.id, { password });
        setMessage(`${user.nickname} 的密码已重置`);
        router.refresh();
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "重置密码失败");
      }
    });
  }

  function handleDisable(user: SalesUser) {
    if (!window.confirm(`确认禁用 ${user.nickname} 吗？`)) {
      return;
    }

    setMessage("");
    startTransition(async () => {
      try {
        await disableSalesUser(user.id);
        setMessage(`${user.nickname} 已禁用`);
        router.refresh();
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "禁用用户失败");
      }
    });
  }

  function handleEnable(user: SalesUser) {
    if (!window.confirm(`确认启用 ${user.nickname} 吗？`)) {
      return;
    }

    setMessage("");
    startTransition(async () => {
      try {
        await enableSalesUser(user.id);
        setMessage(`${user.nickname} 已启用`);
        router.refresh();
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "启用用户失败");
      }
    });
  }

  return (
    <div className="space-y-6">
      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="text-2xl font-semibold">Sales 账户</h2>
            <p className="mt-2 text-sm text-black/65">查看全部 Sales，支持直接重置密码、禁用和重新启用。</p>
          </div>
          <Link className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white" href="/admin/users/new">
            新增 Sales
          </Link>
        </div>
        {message ? <p className="mt-4 text-sm text-black/65">{message}</p> : null}
      </section>

      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        {users.length === 0 ? (
          <p className="rounded-[24px] border border-dashed border-black/10 px-5 py-10 text-center text-sm text-black/55">
            当前还没有 Sales 账户。
          </p>
        ) : (
          <div className="space-y-4">
            {users.map((user) => (
              <article key={user.id} className="rounded-[24px] border border-black/10 bg-[#fffaf5] p-5 shadow-[0_10px_26px_rgba(29,20,13,0.04)]">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div className="space-y-2">
                    <div className="flex flex-wrap items-center gap-3">
                      <span className={user.enabled ? "rounded-full bg-[#ddf4df] px-3 py-1 text-sm font-medium text-[#186a3b]" : "rounded-full bg-[#f7d9d9] px-3 py-1 text-sm font-medium text-[#8a1c1c]"}>
                        {user.enabled ? "启用" : "禁用"}
                      </span>
                      <span className="text-sm text-black/50">{formatDateTime(user.createdAt)}</span>
                    </div>
                    <h3 className="text-xl font-semibold">{user.nickname}</h3>
                    <p className="text-sm text-black/65">{user.email}</p>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium"
                      disabled={isPending}
                      type="button"
                      onClick={() => handleResetPassword(user)}
                    >
                      重置密码
                    </button>
                    <button
                      className={
                        user.enabled
                          ? "rounded-full bg-[#8a1c1c] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
                          : "rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
                      }
                      disabled={isPending}
                      type="button"
                      onClick={() => (user.enabled ? handleDisable(user) : handleEnable(user))}
                    >
                      {user.enabled ? "禁用" : "启用"}
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
