"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState, useTransition } from "react";

import { loginWithEmail } from "@/lib/auth/client";

type LoginFormProps = {
  initialEmail: string;
  nextPath: string;
};

export default function LoginForm({ initialEmail, nextPath }: LoginFormProps) {
  const router = useRouter();
  const [email, setEmail] = useState(initialEmail);
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    startTransition(async () => {
      try {
        await loginWithEmail({ email, password });
        router.replace(nextPath);
        router.refresh();
      } catch (submitError) {
        setError(submitError instanceof Error ? submitError.message : "登录失败");
      }
    });
  }

  return (
    <form className="flex flex-col gap-4 rounded-[28px] bg-white p-6 shadow-[0_12px_30px_rgba(29,20,13,0.08)]" onSubmit={handleSubmit}>
      <label className="flex flex-col gap-2 text-sm font-medium">
        邮箱
        <input
          className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />
      </label>

      <label className="flex flex-col gap-2 text-sm font-medium">
        密码
        <input
          className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
      </label>

      {error ? <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p> : null}

      <button
        className="rounded-2xl bg-[var(--accent)] px-5 py-3 font-semibold text-white transition hover:bg-[var(--accent-strong)] disabled:cursor-not-allowed disabled:opacity-60"
        type="submit"
        disabled={isPending}
      >
        {isPending ? "登录中..." : "登录"}
      </button>

      <p className="text-sm text-black/65">
        还没有账号？{" "}
        <Link className="font-semibold text-[var(--accent)]" href="/register">
          去注册
        </Link>
      </p>
    </form>
  );
}
