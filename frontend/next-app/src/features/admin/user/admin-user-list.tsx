"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, useTransition } from "react";

import { disableMerchantUser, enableMerchantUser, resetMerchantPassword } from "@/lib/admin/client";
import type { LoginLogListResult, MerchantUser } from "@/lib/admin/types";

type AdminUserListProps = {
  users: MerchantUser[];
  loginLogs?: LoginLogListResult;
  loginFilters?: { email?: string; result?: string };
};

const LOGIN_RESULT_OPTIONS = [
  { value: "", label: "全部结果" },
  { value: "SUCCESS", label: "成功" },
  { value: "FAILURE", label: "失败" },
];

export function AdminUserList({ users, loginLogs, loginFilters = {} }: AdminUserListProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [message, setMessage] = useState("");
  const activeTab = searchParams.get("tab") ?? "users";
  const [isPending, startTransition] = useTransition();

  function handleResetPassword(user: MerchantUser) {
    const password = window.prompt(`为 ${user.nickname} 设置新密码（至少 6 位）`, "");
    if (!password) {
      return;
    }

    setMessage("");
    startTransition(async () => {
      try {
        await resetMerchantPassword(user.id, { password });
        setMessage(`${user.nickname} 的密码已重置`);
        router.refresh();
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "重置密码失败");
      }
    });
  }

  function handleDisable(user: MerchantUser) {
    if (!window.confirm(`确认禁用 ${user.nickname} 吗？`)) {
      return;
    }

    setMessage("");
    startTransition(async () => {
      try {
        await disableMerchantUser(user.id);
        setMessage(`${user.nickname} 已禁用`);
        router.refresh();
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "禁用用户失败");
      }
    });
  }

  function handleEnable(user: MerchantUser) {
    if (!window.confirm(`确认启用 ${user.nickname} 吗？`)) {
      return;
    }

    setMessage("");
    startTransition(async () => {
      try {
        await enableMerchantUser(user.id);
        setMessage(`${user.nickname} 已启用`);
        router.refresh();
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "启用用户失败");
      }
    });
  }

  return (
    <div className="space-y-6">
      {/* Tab buttons */}
      <div className="flex gap-3">
        <Link
          className={
            activeTab === "users"
              ? "rounded-full bg-[var(--brand-primary)] px-4 py-2 text-sm font-semibold text-white"
              : "rounded-full border border-[#e0e0e0] px-4 py-2 text-sm font-medium text-[var(--text-secondary)]"
          }
          href="/admin/users"
        >
          Merchant 账户
        </Link>
        <Link
          className={
            activeTab === "login-logs"
              ? "rounded-full bg-[var(--brand-primary)] px-4 py-2 text-sm font-semibold text-white"
              : "rounded-full border border-[#e0e0e0] px-4 py-2 text-sm font-medium text-[var(--text-secondary)]"
          }
          href="/admin/users?tab=login-logs"
        >
          登录日志
        </Link>
      </div>

      {activeTab === "login-logs" && loginLogs ? (
        <LoginLogTable result={loginLogs} filters={loginFilters} />
      ) : (
        <>
          <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h2 className="text-2xl font-semibold">Merchant 账户</h2>
                <p className="mt-2 text-sm text-black/65">查看全部 Merchant，支持直接重置密码、禁用和重新启用。</p>
              </div>
              <Link className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white" href="/admin/users/new">
                新增 Merchant
              </Link>
            </div>
            {message ? <p className="mt-4 text-sm text-black/65">{message}</p> : null}
          </section>

          <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
            {users.length === 0 ? (
              <p className="rounded-[24px] border border-dashed border-black/10 px-5 py-10 text-center text-sm text-black/55">
                当前还没有 Merchant 账户。
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
        </>
      )}
    </div>
  );
}

function LoginLogTable({
  result,
  filters,
}: {
  result: LoginLogListResult;
  filters: { email?: string; result?: string };
}) {
  return (
    <div className="flex flex-col">
      {/* top bar */}
      <div className="flex items-center justify-between border-b border-[#f0f0f0] px-4 py-2">
        <p className="text-sm text-[var(--text-secondary)]">
          本次返回 <span className="font-semibold text-[var(--text-primary)]">{result.items.length}</span> 条
        </p>
      </div>

      {/* filter form */}
      <form action="/admin/users" className="flex items-center gap-3 border-b border-[#f0f0f0] px-4 py-3">
        <input name="tab" type="hidden" value="login-logs" />
        <input
          className="rounded-lg border border-[#e0e0e0] px-3 py-2 text-sm outline-none transition focus:border-[var(--brand-primary)]"
          defaultValue={filters.email ?? ""}
          name="email"
          placeholder="按邮箱精确筛选"
        />
        <select
          className="rounded-lg border border-[#e0e0e0] px-3 py-2 text-sm outline-none transition focus:border-[var(--brand-primary)]"
          defaultValue={filters.result ?? ""}
          name="result"
        >
          {LOGIN_RESULT_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <button
          className="rounded-lg bg-[var(--brand-primary)] px-4 py-2 text-sm font-semibold text-white hover:bg-[var(--brand-deep)]"
          type="submit"
        >
          查询
        </button>
      </form>

      {/* table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#f0f0f0] text-left text-xs text-[var(--text-hint)]">
              <th className="px-4 py-2.5 font-medium">结果</th>
              <th className="px-4 py-2.5 font-medium">邮箱</th>
              <th className="px-4 py-2.5 font-medium">角色快照</th>
              <th className="px-4 py-2.5 font-medium">IP 地址</th>
              <th className="px-4 py-2.5 font-medium">User Agent</th>
              <th className="px-4 py-2.5 font-medium">登录时间</th>
            </tr>
          </thead>
          <tbody>
            {result.items.map((item) => (
              <tr
                key={item.id}
                className="border-b border-[#f5f5f5] transition-colors hover:bg-[#fafafa]"
              >
                <td className="px-4 py-2.5">
                  <span
                    className={`inline-flex rounded-[4px] px-1.5 py-0.5 text-xs font-medium ${
                      item.loginResult === "SUCCESS"
                        ? "bg-emerald-50 text-emerald-700"
                        : "bg-red-50 text-red-700"
                    }`}
                  >
                    {item.loginResult === "SUCCESS" ? "成功" : "失败"}
                  </span>
                </td>
                <td className="px-4 py-2.5 font-medium text-[var(--text-primary)]">
                  {item.emailSnapshot}
                </td>
                <td className="px-4 py-2.5 text-[var(--text-secondary)]">
                  {item.roleSnapshot}
                </td>
                <td className="px-4 py-2.5 text-[var(--text-secondary)]">
                  {item.ipAddress}
                </td>
                <td className="max-w-[200px] truncate px-4 py-2.5 text-[var(--text-hint)]">
                  {item.userAgent ?? "—"}
                </td>
                <td className="whitespace-nowrap px-4 py-2.5 text-[var(--text-hint)]">
                  {formatDateTime(item.loginAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* empty */}
      {result.items.length === 0 ? (
        <p className="px-4 py-10 text-center text-sm text-[var(--text-hint)]">
          当前筛选下没有登录日志
        </p>
      ) : null}
    </div>
  );
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
