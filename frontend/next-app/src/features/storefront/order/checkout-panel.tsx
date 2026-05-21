import { CheckoutAddressCard } from "@/features/storefront/order/checkout-address-card";
import { CheckoutItemList } from "@/features/storefront/order/checkout-item-list";
import { CheckoutBottomBar } from "@/features/storefront/order/checkout-bottom-bar";
import { OrderEmptyState } from "@/features/storefront/order/order-empty-state";
import type { OrderCheckout } from "@/lib/order/types";

type CheckoutPanelProps = {
  checkout: OrderCheckout;
};

export function CheckoutPanel({ checkout }: CheckoutPanelProps) {
  if (checkout.items.length === 0) {
    return (
      <OrderEmptyState
        description="当前没有可进入下单确认的勾选商品。请返回购物车重新勾选。"
        primaryHref="/cart"
        primaryLabel="返回购物车"
        title="没有待确认的订单"
      />
    );
  }

  return (
    <div className="flex flex-col">
      {/* address */}
      <CheckoutAddressCard address={checkout.defaultAddress} />

      {/* item list */}
      <CheckoutItemList items={checkout.items} />

      {/* blocking reasons — inline red box */}
      {checkout.summary.blockingReasons.length > 0 ? (
        <div className="mx-4 mt-3 rounded-lg bg-red-50 px-4 py-3">
          <ul className="flex flex-col gap-1 text-sm text-red-600">
            {checkout.summary.blockingReasons.map((reason) => (
              <li key={reason}>{renderBlockingReason(reason)}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {/* price breakdown */}
      <div className="mx-4 mt-4 space-y-2 border-t border-[#f0f0f0] pt-4 text-sm">
        <div className="flex justify-between">
          <span className="text-[var(--text-secondary)]">商品总额</span>
          <span>{formatPrice(checkout.summary.totalAmount)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-[var(--text-secondary)]">运费</span>
          <span>¥0.00</span>
        </div>
        <div className="flex justify-between border-t border-[#f0f0f0] pt-2 font-semibold">
          <span>合计</span>
          <span
            className="text-[var(--price)]"
            style={{ fontFamily: "var(--font-price)" }}
          >
            {formatPrice(checkout.summary.validTotalAmount)}
          </span>
        </div>
      </div>

      {/* bottom bar */}
      <CheckoutBottomBar canSubmit={checkout.summary.canSubmit} />
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}

function renderBlockingReason(reason: string) {
  switch (reason) {
    case "MISSING_DEFAULT_ADDRESS":
      return "缺少默认收货地址";
    case "NO_SELECTED_ITEMS":
      return "当前没有勾选商品";
    case "PRODUCT_OFF_SHELF":
      return "存在已下架商品";
    case "SKU_INVALID":
      return "存在已失效的 SKU";
    case "SKU_DISABLED":
      return "存在已禁用的 SKU";
    case "INSUFFICIENT_STOCK":
      return "存在库存不足的商品";
    default:
      return reason;
  }
}
