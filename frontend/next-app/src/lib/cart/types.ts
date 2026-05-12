export type ApiErrorResponse = {
  message?: string;
};

export type CartItem = {
  id: number;
  productId: number;
  productName: string;
  productCoverImageUrl: string | null;
  skuId: number;
  skuCode: string;
  skuSpecText: string;
  unitPrice: number;
  quantity: number;
  selected: boolean;
  subtotalAmount: number;
};

export type CartSummary = {
  selectedItemCount: number;
  selectedQuantity: number;
  selectedAmount: number;
};

export type Cart = {
  items: CartItem[];
  summary: CartSummary;
};

export type CartMutation = {
  item: CartItem;
  summary: CartSummary;
};

export type UserAddress = {
  id: number;
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  postalCode: string | null;
  isDefault: boolean;
};

export type UserAddressInput = {
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  postalCode?: string;
};

export type CheckoutItem = {
  id: number;
  productId: number;
  productName: string;
  productCoverImageUrl: string | null;
  skuId: number;
  skuCode: string;
  skuSpecText: string;
  unitPrice: number;
  quantity: number;
  selected: boolean;
  subtotalAmount: number;
  anomalyCode: string | null;
  anomalyMessage: string | null;
  canCheckout: boolean;
};

export type CheckoutSummaryMeta = {
  selectedItemCount: number;
  selectedQuantity: number;
  selectedAmount: number;
  validSelectedItemCount: number;
  validSelectedQuantity: number;
  validSelectedAmount: number;
  canProceed: boolean;
  blockingReasons: string[];
};

export type CheckoutSummary = {
  items: CheckoutItem[];
  defaultAddress: UserAddress | null;
  summary: CheckoutSummaryMeta;
};

export type AddCartItemInput = {
  skuId: number;
  quantity: number;
};

export type UpdateCartItemInput = {
  quantity: number;
  selected: boolean;
};
