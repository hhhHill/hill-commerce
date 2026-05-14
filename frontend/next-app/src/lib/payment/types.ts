export type PaymentApiErrorResponse = {
  message?: string;
};

export type PaymentAttempt = {
  paymentId: number;
  paymentNo: string;
  paymentMethod: string;
  paymentStatus: string;
  amount: number;
  requestedAt: string;
  paidAt: string | null;
  closedAt: string | null;
  failureReason: string | null;
};

export type PaymentOrder = {
  orderId: number;
  orderNo: string;
  orderStatus: string;
  payableAmount: number;
  paymentDeadlineAt: string | null;
  currentAttempt: PaymentAttempt | null;
  attempts: PaymentAttempt[];
};

export type PaymentActionResult = {
  paymentId: number;
  paymentStatus: string;
  orderId: number;
  orderStatus: string;
  paidAt: string | null;
  failureReason: string | null;
};

export type CloseExpiredPaymentsResult = {
  closedOrderCount: number;
};
