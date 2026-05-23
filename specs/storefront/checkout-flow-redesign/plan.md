# Checkout Flow Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将购物车→确认订单→支付→结果的购物流程从卡片式分栏改造为淘宝风格单列紧凑布局。

**Architecture:** 删除 `/checkout-summary` 独立页，合并到确认订单页。三个页面（购物车/确认订单/支付）统一白底+分隔线+底部固定结算栏。不改变任何 API 契约或业务逻辑。

**Tech Stack:** Next.js 15 App Router + TailwindCSS v4 + React 19 (Server Components)

---

## Task 1: 删除 checkout-summary 页面及组件

**Files:**
- Delete: `frontend/next-app/src/app/checkout-summary/page.tsx`
- Delete: `frontend/next-app/src/features/storefront/cart/checkout-summary-panel.tsx`

- [ ] **Step 1: 删除文件**

```bash
cd frontend/next-app
rm src/app/checkout-summary/page.tsx
rm src/features/storefront/cart/checkout-summary-panel.tsx
# 如目录为空则删除
rmdir src/app/checkout-summary 2>/dev/null; true
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend/next-app && npx tsc --noEmit
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: remove checkout-summary page, merging into confirm order"
```

---

## Task 2: 重写购物车页 — 页面布局

**Files:**
- Modify: `frontend/next-app/src/app/cart/page.tsx`

**Description:** 去掉右侧 sidebar 和 chip-badge 描述区，改为淘宝风格：标题简洁 + 单列列表 + 底部固定栏。

- [ ] **Step 1: 替换 cart/page.tsx**

```tsx
import Link from "next/link";

import { CartList } from "@/features/storefront/cart/cart-list";
import { CartBottomBar } from "@/features/storefront/cart/cart-bottom-bar";
import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { getServerCart } from "@/lib/cart/server";

export default async function CartPage() {
  await requireUser("/cart");
  const cart = await getServerCart();

  return (
    <main className="min-h-screen bg-white pb-28">
      <div className="mx-auto max-w-[800px] px-4">
        {/* top bar */}
        <div className="flex items-center justify-between py-3">
          <div className="flex items-center gap-3">
            <HomeShortcut />
            <Link className="text-sm text-[var(--text-secondary)]" href="/categories">
              继续逛逛
            </Link>
          </div>
          <SearchForm />
        </div>

        {/* title */}
        <h1 className="py-2 text-lg font-semibold">购物车</h1>

        {/* list */}
        <CartList items={cart.items} />
      </div>

      {/* bottom bar */}
      <CartBottomBar summary={cart.summary} />
    </main>
  );
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 3: Commit**

---

## Task 3: 重写购物车列表组件

**Files:**
- Modify: `frontend/next-app/src/features/storefront/cart/cart-list.tsx`

**Description:** 商品从卡片包裹改为纯列表行：勾选圈+60x60小图+名称/SKU+价格+加减按钮，行内异常红色文字。

- [ ] **Step 1: 替换 cart-list.tsx**

```tsx
import Link from "next/link";

import { CartItemActions } from "@/features/storefront/cart/cart-item-actions";
import { CartEmptyState } from "@/features/storefront/cart/cart-empty-state";
import type { CartItem } from "@/lib/cart/types";

type CartListProps = {
  items: CartItem[];
};

export function CartList({ items }: CartListProps) {
  if (items.length === 0) {
    return <CartEmptyState />;
  }

  return (
    <div className="flex flex-col">
      {items.map((item) => (
        <div
          key={item.id}
          className="flex items-start gap-3 border-b border-[#f5f5f5] py-3"
        >
          {/* select circle */}
          <div className={`mt-4 h-5 w-5 shrink-0 rounded-full border-2 ${item.canCheckout ? "border-[#ff5000] bg-[#ff5000]" : "border-[#e0e0e0] bg-white"}`} />

          {/* product image */}
          <Link
            className="h-[72px] w-[72px] shrink-0 overflow-hidden rounded bg-[#f5f5f5]"
            href={`/products/${item.productId}`}
          >
            {item.productCoverImageUrl ? (
              <img alt={item.productName} className="h-full w-full object-cover" src={item.productCoverImageUrl} />
            ) : (
              <div className="flex h-full items-center justify-center text-xs text-[var(--text-hint)]">暂无图片</div>
            )}
          </Link>

          {/* info + actions */}
          <div className="flex min-w-0 flex-1 flex-col gap-1">
            <Link className="line-clamp-2 text-sm leading-5" href={`/products/${item.productId}`}>
              {item.productName}
            </Link>
            <p className="text-xs text-[var(--text-hint)]">{item.skuSpecText}</p>

            {/* anomaly hint */}
            {item.canCheckout ? null : (
              <p className="text-xs font-medium text-red-500">{item.anomalyMessage ?? "当前不可结算"}</p>
            )}

            <div className="mt-1 flex items-center justify-between">
              <span className="text-sm font-semibold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
                {formatPrice(item.subtotalAmount)}
              </span>
              <CartItemActions item={item} />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 3: Commit**

---

## Task 4: 创建购物车底部固定栏组件

**Files:**
- Create: `frontend/next-app/src/features/storefront/cart/cart-bottom-bar.tsx`

**Description:** 底部固定栏替代原 cart-summary 组件。全选+合计+结算按钮。

- [ ] **Step 1: 创建 cart-bottom-bar.tsx**

```tsx
"use client";

import Link from "next/link";

import type { CartSummary as CartSummaryModel } from "@/lib/cart/types";

type CartBottomBarProps = {
  summary: CartSummaryModel;
};

export function CartBottomBar({ summary }: CartBottomBarProps) {
  const canCheckout = summary.selectedItemCount > 0;

  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-[#f0f0f0] bg-white">
      <div className="mx-auto flex max-w-[800px] items-center gap-4 px-4 py-3">
        {/* select all */}
        <label className="flex items-center gap-2 text-sm">
          <span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-[#ff5000]">
            <svg className="h-3 w-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path d="M5 13l4 4L19 7" strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} />
            </svg>
          </span>
          全选
        </label>

        <div className="min-w-0 flex-1 text-right">
          <p className="text-xs text-[var(--text-hint)]">
            已选 <span className="font-medium text-[var(--text-primary)]">{summary.selectedItemCount}</span> 件
            {summary.selectedQuantity > summary.selectedItemCount ? ` / ${summary.selectedQuantity} 个` : ""}
            ，不含运费
          </p>
          <p className="text-lg font-bold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
            {formatPrice(summary.selectedAmount)}
          </p>
        </div>

        <Link
          aria-disabled={!canCheckout}
          className={`shrink-0 rounded-full px-8 py-2.5 text-sm font-semibold text-white ${
            canCheckout ? "bg-[#ff5000]" : "bg-[#ccc] pointer-events-none"
          }`}
          href={canCheckout ? "/checkout" : "/cart"}
        >
          结算{canCheckout ? `(${summary.selectedItemCount})` : ""}
        </Link>
      </div>
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 3: Commit**

---

## Task 5: 更新购物车加减按钮样式

**Files:**
- Modify: `frontend/next-app/src/features/storefront/cart/cart-item-actions.tsx`

**Description:** 数量加减从 `rounded-full` 圆形改为方形小按钮，删除按钮改为红色文字。

- [ ] **Step 1: 先读取当前 cart-item-actions.tsx**

Read the file to get exact current code.

- [ ] **Step 2: 替换为淘宝风格按钮**

```tsx
"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { removeFromCart, toggleCartItemSelection, updateCartItemQuantity } from "@/lib/cart/client";
import type { CartItem } from "@/lib/cart/types";

type CartItemActionsProps = {
  item: CartItem;
};

export function CartItemActions({ item }: CartItemActionsProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [optimisticQuantity, setOptimisticQuantity] = useState(item.quantity);

  const displayedQuantity = isPending ? optimisticQuantity : item.quantity;

  function changeQuantity(delta: number) {
    const next = displayedQuantity + delta;
    if (next < 1) return;
    setOptimisticQuantity(next);
    startTransition(async () => {
      try {
        await updateCartItemQuantity(item.id, next);
        router.refresh();
      } catch {
        setOptimisticQuantity(item.quantity);
      }
    });
  }

  function handleDelete() {
    startTransition(async () => {
      try {
        await removeFromCart(item.id);
        router.refresh();
      } catch {
        // silently fail
      }
    });
  }

  return (
    <div className="flex items-center gap-2">
      <button
        className="flex h-6 w-6 items-center justify-center rounded border border-[#e0e0e0] text-sm leading-none text-[var(--text-secondary)]"
        disabled={isPending}
        onClick={() => changeQuantity(-1)}
      >
        −
      </button>
      <span className="min-w-[1.5em] text-center text-sm font-medium">{displayedQuantity}</span>
      <button
        className="flex h-6 w-6 items-center justify-center rounded border border-[#e0e0e0] text-sm leading-none text-[var(--text-secondary)]"
        disabled={isPending}
        onClick={() => changeQuantity(1)}
      >
        +
      </button>
      <button
        className="ml-2 text-xs text-red-400"
        disabled={isPending}
        onClick={handleDelete}
      >
        删除
      </button>
    </div>
  );
}
```

- [ ] **Step 3: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 4: Commit**

---

## Task 6: 重写确认订单页

**Files:**
- Modify: `frontend/next-app/src/app/checkout/page.tsx`

**Description:** 改为淘宝单列风格，去除 sidebar 描述区。

- [ ] **Step 1: 替换 checkout/page.tsx**

```tsx
import Link from "next/link";

import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { CheckoutPanel } from "@/features/storefront/order/checkout-panel";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { getServerOrderCheckout } from "@/lib/order/server";

export default async function CheckoutPage() {
  await requireUser("/checkout");
  const checkout = await getServerOrderCheckout();

  return (
    <main className="min-h-screen bg-white pb-28">
      <div className="mx-auto max-w-[800px] px-4">
        {/* top bar */}
        <div className="flex items-center justify-between py-3">
          <div className="flex items-center gap-3">
            <HomeShortcut />
            <Link className="text-sm text-[var(--text-secondary)]" href="/cart">
              返回购物车
            </Link>
          </div>
          <SearchForm />
        </div>

        {/* title */}
        <h1 className="py-2 text-lg font-semibold">确认订单</h1>

        <CheckoutPanel checkout={checkout} />
      </div>
    </main>
  );
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 3: Commit**

---

## Task 7: 重写确认订单面板 — 单列布局

**Files:**
- Modify: `frontend/next-app/src/features/storefront/order/checkout-panel.tsx`

**Description:** 去掉左右分栏和 sidebar，改为单列垂直。地址紧凑行+商品列表+价格明细+底部固定提交栏。

- [ ] **Step 1: 替换 checkout-panel.tsx**

```tsx
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

      {/* blocking reasons */}
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
          <span className="text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
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
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 3: Commit**

---

## Task 8: 创建确认订单底部提交栏

**Files:**
- Create: `frontend/next-app/src/features/storefront/order/checkout-bottom-bar.tsx`

- [ ] **Step 1: 创建 checkout-bottom-bar.tsx**

```tsx
"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { createOrder } from "@/lib/order/client";

type CheckoutBottomBarProps = {
  canSubmit: boolean;
};

export function CheckoutBottomBar({ canSubmit }: CheckoutBottomBarProps) {
  const router = useRouter();
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  function handleSubmit() {
    setError("");
    startTransition(async () => {
      try {
        const result = await createOrder();
        router.push(`/orders/${result.orderId}/result`);
      } catch (e) {
        setError(e instanceof Error ? e.message : "提交订单失败，请重试");
      }
    });
  }

  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-[#f0f0f0] bg-white">
      <div className="mx-auto flex max-w-[800px] items-center gap-4 px-4 py-3">
        {error ? (
          <p className="flex-1 text-sm text-red-500">{error}</p>
        ) : (
          <p className="flex-1 text-sm text-[var(--text-secondary)]">
            合计：<span className="text-lg font-bold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>见上方明细</span>
          </p>
        )}
        <button
          className={`shrink-0 rounded-full px-10 py-2.5 text-sm font-semibold text-white ${
            canSubmit && !isPending ? "bg-[#ff5000]" : "bg-[#ccc] cursor-not-allowed"
          }`}
          disabled={!canSubmit || isPending}
          onClick={handleSubmit}
        >
          {isPending ? "提交中..." : "提交订单"}
        </button>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 3: Commit**

---

## Task 9: 重写地址卡为紧凑行

**Files:**
- Modify: `frontend/next-app/src/features/storefront/order/checkout-address-card.tsx`

- [ ] **Step 1: 替换 checkout-address-card.tsx**

```tsx
import Link from "next/link";

import type { OrderCheckoutAddress } from "@/lib/order/types";

type CheckoutAddressCardProps = {
  address: OrderCheckoutAddress | null;
};

export function CheckoutAddressCard({ address }: CheckoutAddressCardProps) {
  if (!address) {
    return (
      <Link
        className="mx-4 flex items-center gap-3 border-b border-[#f0f0f0] py-4"
        href="/account/addresses"
      >
        <span className="text-sm font-medium">📍 收货地址</span>
        <span className="text-sm text-red-500">请添加收货地址</span>
        <span className="ml-auto text-[var(--text-hint)]">›</span>
      </Link>
    );
  }

  return (
    <Link
      className="mx-4 flex items-start gap-3 border-b border-[#f0f0f0] py-4"
      href="/account/addresses"
    >
      <span className="mt-0.5 shrink-0 text-sm">📍</span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-3 text-sm">
          <span className="font-semibold">{address.recipientName}</span>
          <span className="text-[var(--text-secondary)]">{address.recipientPhone}</span>
        </div>
        <p className="mt-0.5 text-sm text-[var(--text-secondary)] truncate">
          {address.region}{address.detailAddress}
        </p>
      </div>
      <span className="mt-0.5 shrink-0 text-[var(--text-hint)]">›</span>
    </Link>
  );
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 3: Commit**

---

## Task 10: 重写确认订单商品清单 — 无卡片

**Files:**
- Modify: `frontend/next-app/src/features/storefront/order/checkout-item-list.tsx`

- [ ] **Step 1: 替换 checkout-item-list.tsx**

```tsx
import Link from "next/link";

import type { OrderCheckoutItem } from "@/lib/order/types";

type CheckoutItemListProps = {
  items: OrderCheckoutItem[];
};

export function CheckoutItemList({ items }: CheckoutItemListProps) {
  return (
    <div className="mx-4 flex flex-col">
      {/* shop label */}
      <p className="py-3 text-xs text-[var(--text-hint)]">商品清单</p>

      {items.map((item) => (
        <div
          key={item.id}
          className="flex gap-3 border-b border-[#f5f5f5] py-3"
        >
          {/* product image */}
          <Link
            className="h-[72px] w-[72px] shrink-0 overflow-hidden rounded bg-[#f5f5f5]"
            href={`/products/${item.productId}`}
          >
            {item.productCoverImageUrl ? (
              <img alt={item.productName} className="h-full w-full object-cover" src={item.productCoverImageUrl} />
            ) : (
              <div className="flex h-full items-center justify-center text-xs text-[var(--text-hint)]">暂无图片</div>
            )}
          </Link>

          {/* info */}
          <div className="flex min-w-0 flex-1 flex-col gap-1">
            <Link className="line-clamp-2 text-sm leading-5" href={`/products/${item.productId}`}>
              {item.productName}
            </Link>
            <p className="text-xs text-[var(--text-hint)]">{item.skuSpecText}</p>

            {!item.canSubmit ? (
              <p className="text-xs text-red-500">{item.anomalyMessage ?? "暂不可提交"}</p>
            ) : null}

            <div className="mt-1 flex items-center justify-between">
              <span className="text-sm font-semibold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
                {formatPrice(item.subtotalAmount)}
              </span>
              <span className="text-xs text-[var(--text-hint)]">×{item.quantity}</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 3: Commit**

---

## Task 11: 修改支付页 — 去卡片化

**Files:**
- Modify: `frontend/next-app/src/app/pay/[orderId]/page.tsx`
- Modify: `frontend/next-app/src/features/storefront/payment/payment-panel.tsx`

- [ ] **Step 1: 替换 pay/[orderId]/page.tsx**

```tsx
import Link from "next/link";
import { notFound } from "next/navigation";

import { HomeShortcut } from "@/features/storefront/catalog/home-shortcut";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { PaymentPanel } from "@/features/storefront/payment/payment-panel";
import { requireUser } from "@/lib/auth/server";
import { PaymentRequestError } from "@/lib/payment/errors";
import { getServerPaymentOrder } from "@/lib/payment/server";

type PaymentPageProps = {
  params: Promise<{
    orderId: string;
  }>;
};

export default async function PaymentPage({ params }: PaymentPageProps) {
  const { orderId } = await params;
  await requireUser(`/pay/${orderId}`);

  try {
    const payment = await getServerPaymentOrder(Number(orderId));

    return (
      <main className="min-h-screen bg-white pb-28">
        <div className="mx-auto max-w-[800px] px-4">
          <div className="flex items-center justify-between py-3">
            <div className="flex items-center gap-3">
              <HomeShortcut />
              <Link className="text-sm text-[var(--text-secondary)]" href={`/orders/${orderId}/result`}>
                返回订单结果
              </Link>
            </div>
            <SearchForm />
          </div>

          <h1 className="py-2 text-lg font-semibold">订单支付</h1>

          <PaymentPanel payment={payment} />
        </div>
      </main>
    );
  } catch (error) {
    if (error instanceof PaymentRequestError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}
```

- [ ] **Step 2: 替换 payment-panel.tsx**

```tsx
"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { PaymentActions } from "@/features/storefront/payment/payment-actions";
import { PaymentAttemptCard } from "@/features/storefront/payment/payment-attempt-card";
import { PaymentEmptyState } from "@/features/storefront/payment/payment-empty-state";
import { PaymentStatePanel } from "@/features/storefront/payment/payment-state-panel";
import { createPaymentAttempt, failPayment, succeedPayment } from "@/lib/payment/client";
import type { PaymentOrder } from "@/lib/payment/types";

type PaymentPanelProps = {
  payment: PaymentOrder;
};

export function PaymentPanel({ payment }: PaymentPanelProps) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  const canPay = payment.orderStatus === "PENDING_PAYMENT";
  const canActOnAttempt = canPay && payment.currentAttempt?.paymentStatus === "INITIATED";
  const canCreateAttempt = canPay && (!payment.currentAttempt || payment.currentAttempt.paymentStatus === "FAILED");

  return (
    <div className="flex flex-col">
      {/* order summary */}
      <div className="border-b border-[#f0f0f0] py-4">
        <div className="flex justify-between py-1 text-sm">
          <span className="text-[var(--text-secondary)]">订单号</span>
          <span>{payment.orderNo}</span>
        </div>
        <div className="flex justify-between py-1 text-sm">
          <span className="text-[var(--text-secondary)]">应付金额</span>
          <span className="font-semibold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
            {formatPrice(payment.payableAmount)}
          </span>
        </div>
        <div className="flex justify-between py-1 text-sm">
          <span className="text-[var(--text-secondary)]">订单状态</span>
          <span>{renderOrderStatus(payment.orderStatus)}</span>
        </div>
      </div>

      {/* payment attempt */}
      {payment.currentAttempt ? (
        <PaymentAttemptCard attempt={payment.currentAttempt} />
      ) : (
        <PaymentEmptyState orderId={payment.orderId} />
      )}

      {/* actions */}
      {canPay ? (
        <div className="mt-4 space-y-3">
          <p className="text-sm text-[var(--text-hint)]">支付方式：模拟支付</p>

          <PaymentActions
            busy={isPending}
            canActOnAttempt={canActOnAttempt}
            canCreateAttempt={canCreateAttempt}
            onCreateAttempt={() => {
              setMessage("");
              startTransition(async () => {
                try {
                  await createPaymentAttempt(payment.orderId);
                  router.refresh();
                } catch (error) {
                  setMessage(error instanceof Error ? error.message : "创建支付尝试失败");
                }
              });
            }}
            onFail={() => {
              if (!payment.currentAttempt) return;
              setMessage("");
              startTransition(async () => {
                try {
                  await failPayment(payment.currentAttempt!.paymentId);
                  router.refresh();
                } catch (error) {
                  setMessage(error instanceof Error ? error.message : "支付失败请求未完成");
                }
              });
            }}
            onSucceed={() => {
              if (!payment.currentAttempt) return;
              setMessage("");
              startTransition(async () => {
                try {
                  await succeedPayment(payment.currentAttempt!.paymentId);
                  router.push(`/orders/${payment.orderId}/result`);
                  router.refresh();
                } catch (error) {
                  setMessage(error instanceof Error ? error.message : "支付成功请求未完成");
                }
              });
            }}
          />

          {message ? <p className="text-sm text-red-500">{message}</p> : null}
        </div>
      ) : (
        <PaymentStatePanel orderId={payment.orderId} orderStatus={payment.orderStatus} />
      )}

      {/* nav links */}
      <div className="mt-6 flex gap-3 border-t border-[#f0f0f0] pt-4">
        <Link className="text-sm text-[var(--text-secondary)]" href={`/orders/${payment.orderId}`}>
          查看订单详情
        </Link>
        <Link className="text-sm text-[var(--text-secondary)]" href="/cart">
          返回购物车
        </Link>
      </div>
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}

function renderOrderStatus(status: string) {
  switch (status) {
    case "PENDING_PAYMENT": return "待支付";
    case "PAID": return "已支付";
    case "CANCELLED": return "已取消";
    case "CLOSED": return "已关闭";
    default: return status;
  }
}
```

- [ ] **Step 3: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 4: Commit**

---

## Task 12: 修改支付子组件去卡片化

**Files:**
- Modify: `frontend/next-app/src/features/storefront/payment/payment-summary-card.tsx` — 合并到 payment-panel 中，删除此文件
- Modify: `frontend/next-app/src/features/storefront/payment/payment-attempt-card.tsx` — 去大圆角
- Modify: `frontend/next-app/src/features/storefront/payment/payment-actions.tsx` — 按钮改为 `rounded-full`
- Modify: `frontend/next-app/src/features/storefront/payment/payment-state-panel.tsx` — 去卡片

- [ ] **Step 1: 简化 payment-attempt-card.tsx**

Replace `rounded-[30px] border border-black/10 bg-white/90 shadow-[...]` with plain style:

Read the file first, then replace the article wrapper classes to use flat white bg with bottom border instead of card styling.

- [ ] **Step 2: 简化 payment-actions 按钮样式**

Update button classes from `btn-primary`/`btn-secondary` to `rounded-full` capsule style matching Taobao.

- [ ] **Step 3: 简化 payment-state-panel.tsx**

Remove `rounded-[30px]` card wrapper, use simple text block.

- [ ] **Step 4: 删除 payment-summary-card.tsx** (merged into payment-panel)

- [ ] **Step 5: 验证 TypeScript 编译 + Commit**

---

## Task 13: 修改订单结果页去卡片化

**Files:**
- Modify: `frontend/next-app/src/app/orders/[orderId]/result/page.tsx`
- Modify: `frontend/next-app/src/features/storefront/order/order-result-panel.tsx`

- [ ] **Step 1: 精简 order-result-panel.tsx**

Remove `rounded-[30px]` card wrappers, change to single-column white background layout with centered result icon and compact info.

- [ ] **Step 2: 验证 + Commit**

---

## Task 14: 全局购物流程导航更新

**Files:**
- Modify: 所有引用 `/checkout-summary` 的链接

**Description:** `/checkout-summary` 不存在了，所有跳转改为 `/cart` 或 `/checkout`。

- [ ] **Step 1: 搜索并替换所有 `/checkout-summary` 引用**

```bash
cd frontend/next-app
grep -r "checkout-summary" src/ --include="*.tsx" --include="*.ts" -l
```

对每个文件，将 `/checkout-summary` 替换为 `/cart`（因为汇总页已合并到确认订单，购物车是前一步）。

- [ ] **Step 2: 验证 + Commit**

---

## Execution Order

```
Task 1 (delete) → Task 2-5 (cart, parallel) → Task 6-10 (checkout) → Task 11-12 (pay) → Task 13 (result) → Task 14 (cleanup nav refs)
```

**Total: 14 tasks.** 完成后启动 `npm run dev` 手动验证整个购物流程。
