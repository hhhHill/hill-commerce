// @vitest-environment jsdom

import { cleanup, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminSidebar } from "./admin-sidebar";

vi.mock("next/navigation", () => ({
  usePathname: () => "/admin/products",
  useSearchParams: () => new URLSearchParams(""),
  useRouter: () => ({
    refresh: vi.fn()
  })
}));

afterEach(() => {
  cleanup();
});

describe("AdminSidebar", () => {
  it("hides admin-only items from merchant users", () => {
    render(<AdminSidebar user={{ email: "merchant@example.com", nickname: "Merchant", roles: ["MERCHANT"] }} />);

    // MERCHANT should NOT see admin-only items
    expect(screen.queryByText("仪表盘")).toBeNull();
    expect(screen.queryByText("分类管理")).toBeNull();
    expect(screen.queryByText("用户管理")).toBeNull();
    expect(screen.queryByText("店铺管理")).toBeNull();
    expect(screen.queryByText("首页运营")).toBeNull();

    // MERCHANT should see shared & merchant items
    expect(screen.getByText("我的店铺")).toBeDefined();
    expect(screen.getByText("商品管理")).toBeDefined();
    expect(screen.getByText("订单管理")).toBeDefined();
    expect(screen.getByText("数据分析")).toBeDefined();
    expect(screen.getByText("商品日志")).toBeDefined();
  });

  it("shows admin-only items for admin users", () => {
    render(<AdminSidebar user={{ email: "admin@example.com", nickname: "Admin", roles: ["ADMIN"] }} />);

    // ADMIN should see admin-only items
    expect(screen.getByText("仪表盘")).toBeDefined();
    expect(screen.getByText("用户管理")).toBeDefined();
    expect(screen.getByText("店铺管理")).toBeDefined();
    expect(screen.getByText("首页运营")).toBeDefined();

    // ADMIN should see shared items
    expect(screen.getByText("商品管理")).toBeDefined();
    expect(screen.getByText("订单管理")).toBeDefined();
    expect(screen.getByText("数据分析")).toBeDefined();
    expect(screen.getByText("商品日志")).toBeDefined();

    // ADMIN should NOT see merchant-only items
    expect(screen.queryByText("我的店铺")).toBeNull();
  });
});
