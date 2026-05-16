"use client";

import { useState } from "react";

import { getUserProfile } from "@/lib/admin/analytics-client";
import type { AggregateProfileResponse, UserProfileDetail, UserProfileSummary } from "@/lib/admin/analytics-types";
import { AggregatePanels } from "./aggregate-panels";
import { UserProfileDetailPanel } from "./user-profile-detail";
import { UserSearchBar } from "./user-search-bar";

type ClientUserAnalyticsProps = {
  aggregate: AggregateProfileResponse | null;
};

export function ClientUserAnalytics({ aggregate }: ClientUserAnalyticsProps) {
  const [profile, setProfile] = useState<UserProfileDetail | null>(null);

  const selectUser = async (user: UserProfileSummary) => {
    try {
      setProfile(await getUserProfile(user.userId));
    } catch {
      setProfile(null);
    }
  };

  return (
    <div className="space-y-6">
      <UserSearchBar onSelect={(user) => void selectUser(user)} />
      {profile ? <UserProfileDetailPanel profile={profile} /> : null}
      {aggregate ? <AggregatePanels data={aggregate} /> : <p className="text-sm text-black/55">暂无群体画像数据。</p>}
    </div>
  );
}
