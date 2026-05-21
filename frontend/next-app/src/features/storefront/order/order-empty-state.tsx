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
    <section className="border-b border-gray-100 bg-white px-6 py-10 text-center">
      <div className="mx-auto flex max-w-2xl flex-col items-center gap-4">
        <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
        <p className="text-sm leading-7 text-[var(--text-secondary)]">{description}</p>
        <div className="flex flex-wrap items-center justify-center gap-3">
          <Link className="rounded-full bg-[#ff5000] px-6 py-2.5 text-sm font-medium text-white" href={primaryHref}>
            {primaryLabel}
          </Link>
          {secondaryHref && secondaryLabel ? (
            <Link className="rounded-full border border-[#ff5000] px-6 py-2.5 text-sm font-medium text-[#ff5000]" href={secondaryHref}>
              {secondaryLabel}
            </Link>
          ) : null}
        </div>
      </div>
    </section>
  );
}
