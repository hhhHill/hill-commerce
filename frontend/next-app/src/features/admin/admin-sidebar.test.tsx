// @vitest-environment jsdom

import { cleanup, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminSidebar } from "./admin-sidebar";

vi.mock("next/navigation", () => ({
  usePathname: () => "/admin/products",
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

    expect(screen.queryByText("仪表盘")).toBeNull();
    expect(screen.queryByText("分类管理")).toBeNull();
    expect(screen.queryByText("用户管理")).toBeNull();
    expect(screen.queryByText("店铺管理")).toBeNull();
    expect(screen.getByText("我的店铺")).toBeDefined();
    expect(screen.getByText("商品管理")).toBeDefined();
    expect(screen.getByText("数据分析")).toBeDefined();
  });

  it("shows admin-only items for admin users", () => {
    render(<AdminSidebar user={{ email: "admin@example.com", nickname: "Admin", roles: ["ADMIN"] }} />);

    expect(screen.getByText("分类管理")).toBeDefined();
    expect(screen.getByText("仪表盘")).toBeDefined();
    expect(screen.getByText("用户管理")).toBeDefined();
    expect(screen.getByText("店铺管理")).toBeDefined();
    expect(screen.queryByText("我的店铺")).toBeNull();
  });
});
