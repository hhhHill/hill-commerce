"use client";

import { startTransition, useState } from "react";
import { useRouter } from "next/navigation";

import { recordBrowseEvent, STOREFRONT_BROWSE_EVENTS } from "@/lib/storefront/logging";

type SearchFormProps = {
  defaultKeyword?: string;
  className?: string;
};

export function SearchForm({ defaultKeyword = "", className }: SearchFormProps) {
  const router = useRouter();
  const [keyword, setKeyword] = useState(defaultKeyword);
  const [isPending, setIsPending] = useState(false);

  return (
    <form
      className={`w-full ${className ?? ""}`}
      onSubmit={(event) => {
        event.preventDefault();
        const normalizedKeyword = keyword.trim();
        setIsPending(true);
        recordBrowseEvent(STOREFRONT_BROWSE_EVENTS.searchSubmit, {
          keyword: normalizedKeyword,
          source: "search-form"
        });
        startTransition(() => {
          router.push(normalizedKeyword ? `/search?keyword=${encodeURIComponent(normalizedKeyword)}` : "/search");
          setIsPending(false);
        });
      }}
    >
      <label className="flex w-full items-center gap-2 rounded-[2px] border-2 border-[var(--brand-primary)] bg-[#f5f5f5] px-3 py-2">
        <input
          className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-[var(--text-hint)]"
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜你想要的..."
          value={keyword}
        />
        <button
          className={`shrink-0 rounded-[2px] bg-[var(--brand-primary)] px-4 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-[var(--brand-deep)] ${isPending ? "opacity-70" : ""}`}
          disabled={isPending}
          type="submit"
        >
          {isPending ? "搜索中" : "搜索"}
        </button>
      </label>
    </form>
  );
}
