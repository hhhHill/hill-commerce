"use client";

import type { UserProfileDetail } from "@/lib/admin/analytics-types";

type Props = {
  profile: UserProfileDetail;
};

export function UserProfileDetailPanel({ profile }: Props) {
  return (
    <article className="space-y-4 rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <h3 className="text-lg font-semibold">用户画像详情</h3>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="昵称" value={profile.nickname} />
        <Field label="邮箱" value={profile.email} />
        <Field label="地域" value={profile.region || "未知"} />
        <Field label="累计消费" value={formatCurrency(profile.totalSpent)} />
        <Field label="购买力层级" value={tierLabel(profile.purchasingPowerTier)} />
        <Field label="偏好品类" value={profile.preferredCategories.join("、") || "暂无"} />
        <Field label="近 90 天订单数" value={profile.orderCountLast90Days} />
      </div>
    </article>
  );
}

function Field({ label, value }: { label: string; value: string | number }) {
  return (
    <div>
      <span className="text-sm text-black/55">{label}</span>
      <p className="mt-1 font-semibold">{value}</p>
    </div>
  );
}

function tierLabel(tier: string) {
  if (tier === "low") return "低";
  if (tier === "mid") return "中";
  if (tier === "high") return "高";
  return tier;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(value);
}
