"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState, useTransition } from "react";

import { createSalesUser } from "@/lib/admin/client";

export function AdminUserForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");

    startTransition(async () => {
      try {
        await createSalesUser({ email, nickname, password });
        router.push("/admin/users");
        router.refresh();
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "创建 Sales 失败");
      }
    });
  }

  return (
    <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <h2 className="text-2xl font-semibold">新增 Sales</h2>
      <p className="mt-2 text-sm text-black/65">创建后会自动分配 `SALES` 角色，并立即可用于后台登录。</p>
      <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
        <label className="flex flex-col gap-2 text-sm font-medium">
          邮箱
          <input
            required
            className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
            onChange={(event) => setEmail(event.target.value)}
            placeholder="sales@example.com"
            type="email"
            value={email}
          />
        </label>
        <label className="flex flex-col gap-2 text-sm font-medium">
          昵称
          <input
            required
            className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
            onChange={(event) => setNickname(event.target.value)}
            placeholder="请输入昵称"
            value={nickname}
          />
        </label>
        <label className="flex flex-col gap-2 text-sm font-medium">
          初始密码
          <input
            required
            minLength={6}
            className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
            onChange={(event) => setPassword(event.target.value)}
            placeholder="至少 6 位"
            type="password"
            value={password}
          />
        </label>
        <div className="flex flex-wrap gap-3">
          <button className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white" disabled={isPending} type="submit">
            {isPending ? "提交中..." : "创建账户"}
          </button>
          <Link className="rounded-full border border-black/10 px-5 py-3 text-sm font-medium" href="/admin/users">
            返回用户列表
          </Link>
        </div>
        {message ? <p className="text-sm text-red-700">{message}</p> : null}
      </form>
    </section>
  );
}
