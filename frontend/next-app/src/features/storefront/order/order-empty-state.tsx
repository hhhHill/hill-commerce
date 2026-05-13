import Link from "next/link";

type OrderEmptyStateProps = {
  title: string;
  description: string;
  primaryHref: string;
  primaryLabel: string;
  secondaryHref?: string;
  secondaryLabel?: string;
};

export function OrderEmptyState({
  title,
  description,
  primaryHref,
  primaryLabel,
  secondaryHref,
  secondaryLabel
}: OrderEmptyStateProps) {
  return (
    <section className="rounded-[28px] border border-dashed border-black/15 bg-white/70 px-6 py-10 text-center shadow-[0_16px_50px_rgba(74,42,18,0.06)]">
      <div className="mx-auto flex max-w-2xl flex-col items-center gap-4">
        <span className="rounded-full border border-black/10 bg-[var(--surface)] px-4 py-1 text-xs font-semibold uppercase tracking-[0.22em] text-black/55">
          Order Flow
        </span>
        <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
        <p className="text-sm leading-7 text-black/65">{description}</p>
        <div className="flex flex-wrap items-center justify-center gap-3">
          <Link className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white" href={primaryHref}>
            {primaryLabel}
          </Link>
          {secondaryHref && secondaryLabel ? (
            <Link className="rounded-full border border-black/10 px-5 py-3 text-sm font-medium" href={secondaryHref}>
              {secondaryLabel}
            </Link>
          ) : null}
        </div>
      </div>
    </section>
  );
}
