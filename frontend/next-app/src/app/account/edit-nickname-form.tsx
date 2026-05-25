"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useState, useTransition } from "react";

import { updateProfile } from "@/lib/auth/client";

type EditNicknameFormProps = {
  currentNickname: string;
};

export default function EditNicknameForm({ currentNickname }: EditNicknameFormProps) {
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [nickname, setNickname] = useState(currentNickname);
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    startTransition(async () => {
      try {
        await updateProfile(nickname);
        setEditing(false);
        router.refresh();
      } catch (e) {
        setError(e instanceof Error ? e.message : "更新失败");
      }
    });
  }

  if (!editing) {
    return (
      <div className="flex items-center gap-3">
        <span className="font-semibold">{currentNickname}</span>
        <button
          className="text-xs text-[var(--brand-primary)] hover:underline"
          onClick={() => setEditing(true)}
          type="button"
        >
          修改
        </button>
      </div>
    );
  }

  return (
    <form className="flex items-center gap-2" onSubmit={handleSubmit}>
      <input
        className="rounded-lg border border-black/10 bg-white px-3 py-1.5 text-sm outline-none transition focus:border-[var(--brand-primary)]"
        maxLength={64}
        onChange={(e) => setNickname(e.target.value)}
        required
        type="text"
        value={nickname}
      />
      <button
        className="rounded-full bg-[var(--brand-primary)] px-3 py-1.5 text-xs font-semibold text-white transition hover:opacity-90 disabled:opacity-60"
        disabled={isPending}
        type="submit"
      >
        {isPending ? "保存中..." : "保存"}
      </button>
      <button
        className="text-xs text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
        disabled={isPending}
        onClick={() => {
          setNickname(currentNickname);
          setEditing(false);
        }}
        type="button"
      >
        取消
      </button>
      {error ? <span className="text-xs text-red-600">{error}</span> : null}
    </form>
  );
}
