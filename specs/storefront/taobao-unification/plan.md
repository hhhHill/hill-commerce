# Taobao Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

**Goal:** Unify all storefront pages/components to strict Taobao style: white bg, border-b dividers, 2px radii, no shadows, no chip-badge, no surface-card.

**Architecture:** Mechanical CSS class replacement — no business logic changes. Group by page/component cluster.

**Tech Stack:** Next.js 15 + TailwindCSS v4

---

## Task 1: Fix auth pages (login/register/forbidden)

**Files:**
- `frontend/next-app/src/app/(auth)/login/page.tsx`
- `frontend/next-app/src/app/(auth)/register/page.tsx`
- `frontend/next-app/src/app/forbidden/page.tsx`

Remove `rounded-[32px]`, `rounded-[28px]`, `rounded-[24px]`, `shadow-[...]`, `bg-[var(--surface)]` cards. Replace with white bg + `rounded-[2px]` inputs/buttons + simple dividers.

## Task 2: Fix account page

**Files:**
- `frontend/next-app/src/app/account/page.tsx`

Remove `surface-card`, `chip-badge`, `surface-subtle`. Replace with white bg + flat rows + border dividers.

## Task 3: Fix address management + address-book + address-form

**Files:**
- `frontend/next-app/src/app/account/addresses/page.tsx`
- `frontend/next-app/src/features/storefront/cart/address-book.tsx`
- `frontend/next-app/src/features/storefront/cart/address-form.tsx`

Remove all `rounded-full`, `rounded-[26px]`, `rounded-[24px]`, `rounded-[20px]`, `shadow-[...]`. Replace with `rounded-[2px]` everywhere.

## Task 4: Fix order detail page (single column)

**Files:**
- `frontend/next-app/src/features/storefront/order/order-detail-panel.tsx`

Remove 2-column grid, all `surface-card`/`chip-badge`/`surface-subtle` wrappers. Rearrange to single column with border-b dividers.

## Task 5: Fix order-center components

**Files:**
- `frontend/next-app/src/features/storefront/order-center/order-center-panel.tsx`
- `frontend/next-app/src/features/storefront/order-center/order-center-card.tsx`
- `frontend/next-app/src/features/storefront/order-center/order-center-toolbar.tsx`
- `frontend/next-app/src/features/storefront/order-center/order-center-pagination.tsx`

Remove chip-badge, surface-card, long descriptions. `rounded-[24px]` → `rounded-[2px]`.

## Task 6: Fix catalog components

**Files:**
- `frontend/next-app/src/features/storefront/catalog/empty-state.tsx`
- `frontend/next-app/src/features/storefront/catalog/product-list.tsx`
- `frontend/next-app/src/features/storefront/catalog/search-form.tsx`

Remove surface-card, chip-badge. `rounded-xl`/`rounded-lg` → `rounded-[2px]`.

## Task 7: Fix mobile-nav + add-to-cart-panel

**Files:**
- `frontend/next-app/src/components/mobile-bottom-nav.tsx`
- `frontend/next-app/src/features/storefront/cart/add-to-cart-panel.tsx`

Remove surface-subtle. `rounded-2xl`/`rounded-3xl` → `rounded-[2px]`.

## Task 8: Color token unification

**Files:**
- `frontend/next-app/src/features/storefront/payment/payment-attempt-card.tsx`
- `frontend/next-app/src/features/storefront/payment/payment-state-panel.tsx`
- `frontend/next-app/src/features/storefront/order/order-result-panel.tsx`

Replace `text-black/50` → `text-[var(--text-secondary)]`, `text-black/65` → `text-[var(--text-secondary)]`, `border-black/6` → `border-[#f0f0f0]`, `border-black/10` → `border-[#f0f0f0]`.

## Execution Order

All 8 tasks are file-disjoint (no overlapping files), so they can run largely independent. Execute sequentially per the subagent-driven-development process.
