import type { Metadata } from "next";

import { MobileBottomNav } from "@/components/mobile-bottom-nav";
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

  if (user) {
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
        <div className="min-h-screen pb-20 md:pb-0">{children}</div>
        <MobileBottomNav cartQuantity={cartQuantity} isAuthenticated={Boolean(user)} />
      </body>
    </html>
  );
}
