"use client";

import { FieldLabel } from "./field-label";
import { type FieldHelpPage } from "./field-help";

type FieldHelpRowHeadingsProps = {
  items: Array<{
    field: string;
    label: string;
  }>;
  page: FieldHelpPage;
};

export function FieldHelpRowHeadings({ items, page }: FieldHelpRowHeadingsProps) {
  return (
    <div className="hidden gap-3 text-xs font-semibold uppercase tracking-[0.14em] text-black/45 md:grid">
      {items.map((item) => (
        <div key={`${page}-${item.field}`} className="min-w-0">
          <FieldLabel field={item.field} page={page}>
            {item.label}
          </FieldLabel>
        </div>
      ))}
    </div>
  );
}
