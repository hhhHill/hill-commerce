import type { ReactNode } from "react";

type AdminShellProps = {
  title: string;
  description: string;
  user?: unknown;
  children: ReactNode;
};

export function AdminShell({ title, description, children }: AdminShellProps) {
  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-[var(--text-primary)]">{title}</h1>
        <p className="mt-1 text-sm text-[var(--text-secondary)]">{description}</p>
      </div>
      {children}
    </div>
  );
}
