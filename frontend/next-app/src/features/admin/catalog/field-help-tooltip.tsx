"use client";

import { getFieldHelp, type FieldHelpPage } from "./field-help";

type FieldHelpTooltipProps = {
  page: FieldHelpPage;
  field: string;
};

export function FieldHelpTooltip({ page, field }: FieldHelpTooltipProps) {
  const help = getFieldHelp(page, field);

  if (!help) {
    return null;
  }

  return (
    <span className="group relative inline-flex align-middle">
      <span
        aria-label={`${help.title} 字段说明`}
        className="inline-flex h-5 w-5 cursor-help items-center justify-center rounded-full border border-black/15 bg-white text-xs font-semibold text-black/65 transition group-hover:border-[var(--accent)] group-hover:text-[var(--accent-strong)]"
        tabIndex={0}
      >
        ?
      </span>
      <span className="pointer-events-none absolute left-full top-1/2 z-20 ml-3 hidden w-64 -translate-y-1/2 rounded-2xl border border-black/10 bg-white px-4 py-3 text-xs font-normal leading-5 text-black shadow-[0_18px_38px_rgba(29,20,13,0.14)] group-hover:block group-focus-within:block">
        <strong className="block text-sm text-black">{help.title}</strong>
        <span className="mt-1 block text-black/70">{help.description}</span>
      </span>
    </span>
  );
}
