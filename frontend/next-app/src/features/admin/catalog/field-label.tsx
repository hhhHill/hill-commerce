"use client";

import { type ReactNode } from "react";

import { FieldHelpTooltip } from "./field-help-tooltip";
import { type FieldHelpPage } from "./field-help";

type FieldLabelProps = {
  children: ReactNode;
  field: string;
  page: FieldHelpPage;
};

export function FieldLabel({ children, page, field }: FieldLabelProps) {
  return (
    <span className="inline-flex items-center gap-2">
      <span>{children}</span>
      <FieldHelpTooltip field={field} page={page} />
    </span>
  );
}
