import Link from "next/link";
import { redirect } from "next/navigation";
import { Suspense } from "react";

import { AdminSidebar } from "@/features/admin/admin-sidebar";
import { getSessionUser } from "@/lib/auth/server";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const user = await getSessionUser();
  if (!user) {
    redirect("/login");
  }
  const isAdmin = user.roles.includes("ADMIN");
  const isMerchant = user.roles.includes("MERCHANT");
  if (!isAdmin && !isMerchant) {
    redirect("/forbidden");
  }

  return (
    <div className="flex h-screen flex-col bg-[var(--bg-page)]">
      {/* Top bar */}
      <header className="flex h-12 shrink-0 items-center justify-between border-b border-[var(--border-normal)] bg-white px-6">
        <Link className="text-base font-bold text-[var(--text-primary)]" href="/admin">
          Hill Commerce Admin
        </Link>
        <div className="flex items-center gap-4 text-xs text-[var(--text-secondary)]">
          <span>{user.nickname}</span>
          <Link className="text-[var(--brand-primary)] hover:underline" href="/">
            返回首页
          </Link>
        </div>
      </header>

      {/* Body: sidebar + content */}
      <div className="flex flex-1 overflow-hidden">
        <Suspense fallback={<div className="w-[240px] shrink-0 border-r border-[var(--border-normal)] bg-white" />}>
          <AdminSidebar user={user} />
        </Suspense>
        <main className="flex-1 overflow-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
