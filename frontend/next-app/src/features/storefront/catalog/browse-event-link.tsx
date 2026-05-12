"use client";

import Link from "next/link";
import type { ComponentProps, MouseEvent, PropsWithChildren } from "react";

import { recordBrowseEvent, type BrowseEventPayload } from "@/lib/storefront/logging";

type BrowseEventLinkProps = PropsWithChildren<
  ComponentProps<typeof Link> & {
    className?: string;
    eventName: string;
    eventPayload?: BrowseEventPayload;
  }
>;

export function BrowseEventLink({ children, className, eventName, eventPayload, ...linkProps }: BrowseEventLinkProps) {
  return (
    <Link
      {...linkProps}
      className={className}
      onClick={(event: MouseEvent<HTMLAnchorElement>) => {
        recordBrowseEvent(eventName as never, eventPayload);
        linkProps.onClick?.(event);
      }}
    >
      {children}
    </Link>
  );
}
