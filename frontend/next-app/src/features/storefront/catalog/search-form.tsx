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
      className={className}
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
      <label className="flex items-center gap-3 rounded-full border border-black/10 bg-white/80 px-4 py-3 shadow-[0_10px_30px_rgba(74,42,18,0.08)]">
        <span className="text-xs font-semibold uppercase tracking-[0.18em] text-black/45">Search</span>
        <input
          className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-black/35"
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="按商品名称搜索"
          value={keyword}
        />
        <button
          className="rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          disabled={isPending}
          type="submit"
        >
          {isPending ? "搜索中" : "搜索"}
        </button>
      </label>
    </form>
  );
}
