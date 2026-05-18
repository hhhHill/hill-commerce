import type { StorefrontProductCard } from "@/lib/storefront/types";

// These values are front-end merchandising placeholders only. They do not
// represent real original prices, sales counts, shop ownership, or promotions.
export type ProductCardViewModel = {
  price: string;
  originalPrice: string | null;
  sales: string;
  shopName: string;
  tags: string[];
};

const TAG_GROUPS = [
  ["百亿补贴", "包邮", "官方"],
  ["限时", "自营", "包邮"],
  ["官方", "爆款", "包邮"],
  ["百亿补贴", "自营", "限时"]
];

const SHOP_NAMES = ["官方旗舰店", "品牌直营店", "精选好店", "官方自营店", "品质生活馆"];

export function buildProductCardViewModel(product: StorefrontProductCard): ProductCardViewModel {
  const numericPrice = parsePrice(product.salePrice);

  return {
    price: product.salePrice ?? "待定价",
    originalPrice: numericPrice === null ? null : deriveOriginalPrice(numericPrice, product.id),
    sales: deriveSales(product.id),
    shopName: SHOP_NAMES[(product.categoryId + product.id) % SHOP_NAMES.length],
    tags: TAG_GROUPS[product.id % TAG_GROUPS.length]
  };
}

function parsePrice(price: string | null): number | null {
  if (!price) {
    return null;
  }

  const value = Number(price);
  return Number.isFinite(value) ? value : null;
}

function deriveOriginalPrice(price: number, productId: number): string {
  const multiplier = 1.18 + (productId % 5) * 0.08;
  return (Math.ceil(price * multiplier * 10) / 10).toFixed(2);
}

function deriveSales(productId: number): string {
  const base = 1200 + (productId * 137) % 98000;
  if (base >= 10000) {
    return `已售 ${(base / 10000).toFixed(1)}w+`;
  }
  return `已售 ${base}+`;
}
