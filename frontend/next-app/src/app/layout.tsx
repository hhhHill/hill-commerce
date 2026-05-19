import type { Metadata } from "next";

import { MobileBottomNav } from "@/components/mobile-bottom-nav";
import { StorefrontNav } from "@/features/storefront/nav/storefront-nav";
import { getSessionUser } from "@/lib/auth/server";
import { getServerCart } from "@/lib/cart/server";

import "./globals.css";

export const metadata: Metadata = {
  title: "hill-commerce",
  description: "MVP storefront for hill-commerce"
};

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const user = await getSessionUser();
  let cartQuantity = 0;
  let userProp: { nickname: string } | null = null;

  if (user) {
    userProp = { nickname: user.nickname };
    try {
      const cart = await getServerCart();
      cartQuantity = cart.items.reduce((total, item) => total + item.quantity, 0);
    } catch {
      cartQuantity = 0;
    }
  }

  return (
    <html lang="zh-CN">
      <body className="min-h-screen bg-[var(--bg-page)] text-[var(--text-primary)]">
        <StorefrontNav cartQuantity={cartQuantity} user={userProp} />
        <div className="min-h-screen pb-20 md:pb-0">{children}</div>
        <MobileBottomNav cartQuantity={cartQuantity} isAuthenticated={Boolean(user)} />
      </body>
    </html>
  );
}
