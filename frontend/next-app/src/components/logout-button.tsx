"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { logoutCurrentUser } from "@/lib/auth/client";

type LogoutButtonProps = {
  className?: string;
};

export function LogoutButton({ className }: LogoutButtonProps) {
  const router = useRouter();
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  return (
    <div className="flex flex-col gap-2">
      <button
        className={className}
        type="button"
        disabled={isPending}
        onClick={() => {
          setError("");
          startTransition(async () => {
            try {
              await logoutCurrentUser();
              router.replace("/login");
              router.refresh();
            } catch (logoutError) {
              setError(logoutError instanceof Error ? logoutError.message : "退出失败");
            }
          });
        }}
      >
        {isPending ? "退出中..." : "退出登录"}
      </button>
      {error ? <p className="text-sm text-red-700">{error}</p> : null}
    </div>
  );
}
