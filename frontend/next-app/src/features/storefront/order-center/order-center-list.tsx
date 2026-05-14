import { OrderCenterCard } from "@/features/storefront/order-center/order-center-card";
import type { OrderListItem } from "@/lib/order/types";

type OrderCenterListProps = {
  items: OrderListItem[];
};

export function OrderCenterList({ items }: OrderCenterListProps) {
  return (
    <div className="flex flex-col gap-4">
      {items.map((order) => (
        <OrderCenterCard key={order.orderId} order={order} />
      ))}
    </div>
  );
}
