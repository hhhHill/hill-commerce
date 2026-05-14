export type ApiErrorResponse = {
  message?: string;
};

export type OrderListStatus =
  | "PENDING_PAYMENT"
  | "PAID"
  | "SHIPPED"
  | "COMPLETED"
  | "CANCELLED"
  | "CLOSED";

export type OrderListQuery = {
  page?: number;
  size?: number;
  status?: OrderListStatus;
  orderNo?: string;
};

export type OrderListItem = {
  orderId: number;
  orderNo: string;
  orderStatus: OrderListStatus;
  payableAmount: number;
  createdAt: string;
  summaryProductName: string | null;
  summaryItemCount: number;
};

export type OrderListResult = {
  items: OrderListItem[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
};

export type OrderCheckoutAddress = {
  id: number | null;
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  postalCode: string | null;
  isDefault: boolean;
};

export type OrderCheckoutItem = {
  id: number;
  productId: number;
  productName: string;
  productCoverImageUrl: string | null;
  skuId: number;
  skuCode: string;
  skuSpecText: string;
  unitPrice: number;
  quantity: number;
  subtotalAmount: number;
  anomalyCode: string | null;
  anomalyMessage: string | null;
  canSubmit: boolean;
};

export type OrderCheckoutSummaryMeta = {
  selectedItemCount: number;
  selectedQuantity: number;
  totalAmount: number;
  validSelectedItemCount: number;
  validSelectedQuantity: number;
  validTotalAmount: number;
  canSubmit: boolean;
  blockingReasons: string[];
};

export type OrderCheckout = {
  items: OrderCheckoutItem[];
  defaultAddress: OrderCheckoutAddress | null;
  summary: OrderCheckoutSummaryMeta;
};

export type CreateOrderResult = {
  orderId: number;
  orderNo: string;
  orderStatus: string;
  payableAmount: number;
};

export type OrderLineItem = {
  id: number;
  productId: number;
  skuId: number;
  productName: string;
  skuCode: string;
  skuSpecText: string;
  productImageUrl: string | null;
  unitPrice: number;
  quantity: number;
  subtotalAmount: number;
};

export type OrderStatusHistory = {
  id: number;
  fromStatus: string | null;
  toStatus: string;
  changedBy: number | null;
  changeReason: string | null;
  createdAt: string;
};

export type ShipmentInfo = {
  carrierName: string;
  trackingNo: string;
  shippedAt: string | null;
};

export type OrderDetail = {
  id: number;
  orderNo: string;
  orderStatus: string;
  totalAmount: number;
  payableAmount: number;
  paymentDeadlineAt: string | null;
  address: OrderCheckoutAddress;
  items: OrderLineItem[];
  statusHistory: OrderStatusHistory[];
  shipment: ShipmentInfo | null;
};

export type CancelOrderResult = {
  orderId: number;
  orderStatus: string;
};

export type ConfirmReceiptResult = {
  orderId: number;
  orderStatus: string;
  shipmentStatus: string;
};
