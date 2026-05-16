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
      <label className="flex w-full items-center gap-3 rounded-full border border-[var(--border-normal)] bg-white px-3 py-2 shadow-[0_2px_8px_rgba(0,0,0,0.04)]">
        <span className="chip-badge shrink-0">搜索</span>
        <input
          className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-[var(--text-hint)]"
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜你想要的..."
          value={keyword}
        />
        <button
          className={`btn-primary shrink-0 px-4 py-2 ${isPending ? "opacity-70" : ""}`}
          disabled={isPending}
          type="submit"
        >
          {isPending ? "搜索中" : "搜索"}
        </button>
      </label>
    </form>
  );
}
