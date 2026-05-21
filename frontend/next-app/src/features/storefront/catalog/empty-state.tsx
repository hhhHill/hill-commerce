import Link from "next/link";

type EmptyStateProps = {
  title: string;
  description: string;
  actionHref?: string;
  actionLabel?: string;
};

export function EmptyState({ title, description, actionHref = "/categories", actionLabel = "继续逛逛" }: EmptyStateProps) {
  return (
    <div className="bg-white border-b border-[#f0f0f0] py-8 text-center">
      <div className="mx-auto flex max-w-xl flex-col items-center gap-4">
        <span className="text-sm text-[var(--text-hint)]">空状态</span>
        <h2 className="text-2xl font-semibold tracking-tight">{title}</h2>
        <p className="text-sm leading-7 text-[var(--text-secondary)]">{description}</p>
        <Link className="rounded-[2px] bg-[#ff5000] text-white px-5 py-2 text-sm font-semibold" href={actionHref}>
          {actionLabel}
        </Link>
      </div>
    </div>
  );
}
