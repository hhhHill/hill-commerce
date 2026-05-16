export type TrendPoint = {
  date: string;
  amount: number;
  movingAvg: number;
  lastPeriodAmount: number;
};

export type TrendResponse = {
  granularity: string;
  points: TrendPoint[];
  trendDirection: string;
  changePercent: number;
};

export type AnomalyItem = {
  id: string;
  snapshotHour: string;
  currentAmount: number;
  baselineMean: number;
  baselineStd: number;
  direction: "high" | "low" | string;
  deviationPercent: number;
};

export type AnomalyStatusResponse = {
  hasAlert: boolean;
  count: number;
};

export type ProductRankItem = {
  productId: number;
  productName: string;
  categoryId: number;
  categoryName: string;
  totalQuantity: number;
  totalAmount: number;
};

export type ProductRankingResponse = {
  range: string;
  items: ProductRankItem[];
};

export type RegionDistribution = {
  region: string;
  userCount: number;
};

export type PurchasingPowerTier = {
  tier: string;
  userCount: number;
  totalAmount: number;
};

export type CategoryPreference = {
  categoryId: number;
  categoryName: string;
  orderCount: number;
};

export type AggregateProfileResponse = {
  regionDistribution: RegionDistribution[];
  purchasingPowerTiers: PurchasingPowerTier[];
  categoryPreferences: CategoryPreference[];
  totalUsers: number;
  repeatPurchaseUsers: number;
  repeatPurchaseRate: number;
};

export type UserProfileSummary = {
  userId: number;
  email: string;
  nickname: string;
};

export type UserProfileDetail = {
  userId: number;
  email: string;
  nickname: string;
  region: string;
  totalSpent: number;
  purchasingPowerTier: string;
  preferredCategories: string[];
  orderCountLast90Days: number;
};
