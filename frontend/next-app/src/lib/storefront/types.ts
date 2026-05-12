export type StorefrontSaleStatus = "AVAILABLE" | "OUT_OF_STOCK" | "UNAVAILABLE" | "OFF_SHELF";
export type StorefrontSkuStatus = "ENABLED" | "DISABLED";
export type StorefrontStockStatus = "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK";

export type StorefrontCategory = {
  id: number;
  name: string;
};

export type StorefrontProductCard = {
  id: number;
  categoryId: number;
  name: string;
  salePrice: string | null;
  coverImageUrl: string | null;
};

export type StorefrontPagedResponse<T> = {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
};

export type StorefrontSalesAttributeValue = {
  id: number;
  value: string;
  sortOrder: number;
};

export type StorefrontSalesAttribute = {
  id: number;
  name: string;
  sortOrder: number;
  values: StorefrontSalesAttributeValue[];
};

export type StorefrontProductSku = {
  id: number;
  skuCode: string;
  salesAttrValueKey: string;
  salesAttrValueText: string;
  price: string;
  stock: number;
  lowStockThreshold: number;
  stockStatus: StorefrontStockStatus;
  status: StorefrontSkuStatus;
};

export type StorefrontProductDetail = {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  subtitle: string | null;
  coverImageUrl: string | null;
  detailImages: string[];
  salePrice: string | null;
  saleStatus: StorefrontSaleStatus;
  description: string | null;
  salesAttributes: StorefrontSalesAttribute[];
  skus: StorefrontProductSku[];
};

export type StorefrontListParams = {
  page?: number;
  pageSize?: number;
};

export type StorefrontSearchParams = StorefrontListParams & {
  keyword: string;
};

export type ApiErrorResponse = {
  message?: string;
};
