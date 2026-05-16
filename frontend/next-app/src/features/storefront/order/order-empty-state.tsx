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
    <section className="surface-card border-dashed px-6 py-10 text-center">
      <div className="mx-auto flex max-w-2xl flex-col items-center gap-4">
        <span className="chip-badge">订单流程</span>
        <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
        <p className="text-sm leading-7 text-[var(--text-secondary)]">{description}</p>
        <div className="flex flex-wrap items-center justify-center gap-3">
          <Link className="btn-primary px-5 py-3" href={primaryHref}>
            {primaryLabel}
          </Link>
          {secondaryHref && secondaryLabel ? (
            <Link className="btn-secondary px-5 py-3" href={secondaryHref}>
              {secondaryLabel}
            </Link>
          ) : null}
        </div>
      </div>
    </section>
  );
}
