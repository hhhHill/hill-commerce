export type CategoryStatus = "ENABLED" | "DISABLED";
export type ProductStatus = "DRAFT" | "ON_SHELF" | "OFF_SHELF";
export type SkuStatus = "ENABLED" | "DISABLED";

export type Category = {
  id: number;
  name: string;
  sortOrder: number;
  status: CategoryStatus;
};

export type ProductImage = {
  id?: number;
  imageUrl: string;
  sortOrder: number;
};

export type ProductAttribute = {
  id?: number;
  name: string;
  value: string;
  sortOrder: number;
};

export type ProductSalesAttributeValue = {
  id?: number;
  value: string;
  sortOrder: number;
};

export type ProductSalesAttribute = {
  id?: number;
  name: string;
  sortOrder: number;
  values: ProductSalesAttributeValue[];
};

export type ProductSku = {
  id?: number;
  skuCode: string;
  salesAttrValueKey: string;
  salesAttrValueText: string;
  price: string;
  stock: number;
  lowStockThreshold: number;
  status: SkuStatus;
};

export type ProductSummary = {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  spuCode: string;
  status: ProductStatus;
  minSalePrice: string | null;
  coverImageUrl: string | null;
};

export type ProductDetail = {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  spuCode: string;
  subtitle: string | null;
  coverImageUrl: string | null;
  description: string | null;
  status: ProductStatus;
  minSalePrice: string | null;
  detailImages: ProductImage[];
  attributes: ProductAttribute[];
  salesAttributes: ProductSalesAttribute[];
  skus: ProductSku[];
};

export type ProductPayload = {
  categoryId: number;
  name: string;
  spuCode: string;
  subtitle: string;
  coverImageUrl: string;
  description: string;
  status: ProductStatus;
  detailImages: ProductImage[];
  attributes: ProductAttribute[];
  salesAttributes: ProductSalesAttribute[];
  skus: ProductSku[];
};

export type ProductListFilters = {
  name?: string;
  categoryId?: string;
  status?: string;
};

export type ApiErrorResponse = {
  message?: string;
};
