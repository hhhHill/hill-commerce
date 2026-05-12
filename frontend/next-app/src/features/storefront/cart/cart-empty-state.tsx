import { EmptyState } from "@/features/storefront/catalog/empty-state";

type CartEmptyStateProps = {
  title?: string;
  description?: string;
  actionHref?: string;
  actionLabel?: string;
};

export function CartEmptyState({
  title = "购物车还是空的",
  description = "先去挑一件商品加进来，后面的地址和结算准备才有真实场景。",
  actionHref = "/categories",
  actionLabel = "去逛商品"
}: CartEmptyStateProps) {
  return <EmptyState actionHref={actionHref} actionLabel={actionLabel} description={description} title={title} />;
}
