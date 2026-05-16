"use client";

import { useState } from "react";

import { searchUsers } from "@/lib/admin/analytics-client";
import type { UserProfileSummary } from "@/lib/admin/analytics-types";

type UserSearchBarProps = {
  onSelect: (user: UserProfileSummary) => void;
};

export function UserSearchBar({ onSelect }: UserSearchBarProps) {
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<UserProfileSummary[]>([]);
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    if (!keyword.trim()) return;
    setLoading(true);
    try {
      setResults(await searchUsers(keyword.trim()));
    } catch {
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative">
      <div className="flex flex-wrap gap-2">
        <input
          className="w-72 rounded-full border border-[var(--border-normal)] px-4 py-2 text-sm focus:border-[var(--brand-primary)] focus:outline-none"
          onChange={(event) => setKeyword(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") void handleSearch();
          }}
          placeholder="搜索用户邮箱或昵称..."
          value={keyword}
        />
        <button className="btn-primary px-4 py-2 text-sm" disabled={loading} onClick={() => void handleSearch()} type="button">
          {loading ? "搜索中..." : "搜索"}
        </button>
      </div>
      {results.length > 0 ? (
        <div className="absolute left-0 top-12 z-10 max-h-64 w-80 overflow-auto rounded-xl border border-[var(--border-normal)] bg-white shadow-lg">
          {results.map((user) => (
            <button
              key={user.userId}
              className="block w-full px-4 py-3 text-left text-sm hover:bg-[#fffaf5]"
              onClick={() => {
                onSelect(user);
                setResults([]);
                setKeyword("");
              }}
              type="button"
            >
              <span className="font-semibold">{user.nickname}</span>
              <span className="ml-2 text-black/45">{user.email}</span>
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}
