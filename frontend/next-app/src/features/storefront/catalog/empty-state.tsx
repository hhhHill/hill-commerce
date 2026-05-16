import Link from "next/link";

type EmptyStateProps = {
  title: string;
  description: string;
  actionHref?: string;
  actionLabel?: string;
};

export function EmptyState({ title, description, actionHref = "/categories", actionLabel = "继续逛逛" }: EmptyStateProps) {
  return (
    <div className="surface-card border-dashed px-6 py-10 text-center">
      <div className="mx-auto flex max-w-xl flex-col items-center gap-4">
        <span className="chip-badge">空状态</span>
        <h2 className="text-2xl font-semibold tracking-tight">{title}</h2>
        <p className="text-sm leading-7 text-[var(--text-secondary)]">{description}</p>
        <Link className="btn-primary px-5 py-2" href={actionHref}>
          {actionLabel}
        </Link>
      </div>
    </div>
  );
}
