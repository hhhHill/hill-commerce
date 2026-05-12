import Link from "next/link";

type EmptyStateProps = {
  title: string;
  description: string;
  actionHref?: string;
  actionLabel?: string;
};

export function EmptyState({ title, description, actionHref = "/categories", actionLabel = "继续逛逛" }: EmptyStateProps) {
  return (
    <div className="rounded-[28px] border border-dashed border-black/15 bg-white/70 px-6 py-10 text-center shadow-[0_16px_50px_rgba(74,42,18,0.06)]">
      <div className="mx-auto flex max-w-xl flex-col items-center gap-4">
        <span className="rounded-full border border-black/10 bg-[var(--surface)] px-4 py-1 text-xs font-semibold uppercase tracking-[0.22em] text-black/55">
          Empty State
        </span>
        <h2 className="text-2xl font-semibold tracking-tight">{title}</h2>
        <p className="text-sm leading-7 text-black/65">{description}</p>
        <Link className="rounded-full bg-[var(--accent)] px-5 py-2 text-sm font-semibold text-white" href={actionHref}>
          {actionLabel}
        </Link>
      </div>
    </div>
  );
}
