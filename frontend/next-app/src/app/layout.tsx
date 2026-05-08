import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "hill-commerce",
  description: "MVP storefront for hill-commerce"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
