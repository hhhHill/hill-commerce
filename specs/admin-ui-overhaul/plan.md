# Admin 后台界面改版 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将管理后台三个核心区域改造为高信息密度、高效率的操作界面：首页运营合并分类管理、商品列表紧凑表格化、新建商品交互全面优化。

**Architecture:** 前端 Next.js 15 App Router + 后端 Spring Boot。改动以前端为主，后端仅加商品列表分页支持（添加 page/size 参数，返回分页结构）。数据库不变，API 签名尽量兼容。

**Tech Stack:** Next.js 15.5, React 19, TypeScript, Tailwind CSS 4, Java Spring Boot, MyBatis-Plus

---

## 文件结构

```
修改:
  backend/.../web/ProductAdminController.java  — 列表接口加分页参数
  backend/.../service/ProductAdminService.java — 分页查询逻辑
  backend/.../dto/ProductAdminDtos.java        — 新增 ProductListResponse 分页包装

  frontend/next-app/src/lib/admin/types.ts     — 新增 ProductListResult 类型
  frontend/next-app/src/lib/admin/server.ts    — getAdminProducts 支持分页
  frontend/next-app/src/lib/admin/client.ts    — 不变（客户端不直接调列表 API）

  frontend/next-app/src/app/admin/products/page.tsx      — 分页参数透传
  frontend/next-app/src/features/admin/catalog/product-list.tsx — 卡片→表格重写
  frontend/next-app/src/features/admin/catalog/product-editor.tsx — 全面重写

  frontend/next-app/src/app/admin/homepage/page.tsx                — 合并分类数据
  frontend/next-app/src/features/admin/homepage/homepage-form.tsx  — 加入分类表格

  frontend/next-app/src/features/admin/admin-sidebar.tsx — 移除分类管理入口

新增:
  frontend/next-app/src/features/admin/catalog/batch-sku-input.tsx — SKU 批量填充

删除:
  frontend/next-app/src/app/admin/categories/page.tsx             — 合并到首页运营
  frontend/next-app/src/features/admin/catalog/category-manager.tsx — 不再需要
  frontend/next-app/src/features/admin/catalog/sku-grid-layout.tsx  — 内联到 product-editor
```

---

### Task 1: 后端 — 商品列表加分页 DTO

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/dto/ProductAdminDtos.java`

- [ ] **Step 1: 在 ProductAdminDtos 中新增 ProductListResponse record**

找到 ProductAdminDtos.java 中 `ProductSummaryResponse` 的定义位置，在其后添加：

```java
public record ProductListResponse(
    List<ProductSummaryResponse> items,
    int page,
    int size,
    long total,
    int totalPages
) {}
```

import 需要 `java.util.List`（文件内已有）。

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/product/dto/ProductAdminDtos.java
git commit -m "feat: add ProductListResponse DTO for paginated product list"
```

---

### Task 2: 后端 — ProductAdminService 加分页查询

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/service/ProductAdminService.java`

- [ ] **Step 1: 修改 listProducts 方法签名和实现**

将 `listProducts` 方法签名改为：

```java
public ProductListResponse listProducts(String name, Long categoryId, String status, Long shopId, int page, int size) {
```

在方法体内，将 `productMapper.selectList(queryWrapper)` 替换为分页查询：

```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

// 在 queryWrapper 构建完成后，将:
// List<ProductEntity> products = productMapper.selectList(queryWrapper);
// 替换为:

Page<ProductEntity> pageObj = new Page<>(page, size);
Page<ProductEntity> resultPage = productMapper.selectPage(pageObj, queryWrapper);
List<ProductEntity> products = resultPage.getRecords();

// 在 return 语句处，改为返回 ProductListResponse:
return new ProductListResponse(
    summaries,  // ProductSummaryResponse 列表（原返回值）
    page,
    size,
    resultPage.getTotal(),
    (int) Math.ceil((double) resultPage.getTotal() / size)
);
```

注意：原方法最后一行是 `return products.stream().map(...).collect(...)`，需要把 `.collect(Collectors.toList())` 的结果赋给变量 `summaries` 再包装进 `ProductListResponse`。

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvnw compile -q
```

Expected: BUILD SUCCESS (需要检查变量名和 import)

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/product/service/ProductAdminService.java
git commit -m "feat: add pagination support to product list query"
```

---

### Task 3: 后端 — ProductAdminController 加分页参数

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/web/ProductAdminController.java`

- [ ] **Step 1: 修改 listProducts 端点**

将方法签名从返回 `List<ProductSummaryResponse>` 改为返回 `ProductListResponse`，添加 `page` 和 `size` 请求参数（带默认值）：

```java
import static com.hillcommerce.modules.product.dto.ProductAdminDtos.ProductListResponse;

@GetMapping
@RequireRole({"ADMIN", "MERCHANT"})
public ProductListResponse listProducts(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) Long categoryId,
    @RequestParam(required = false) String status,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
) {
    return productAdminService.listProducts(name, categoryId, status, ShopContext.currentShopId(), page, size);
}
```

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/product/web/ProductAdminController.java
git commit -m "feat: add page/size params to product list API"
```

---

### Task 4: 前端 — 类型定义加分页类型

**Files:**
- Modify: `frontend/next-app/src/lib/admin/types.ts`

- [ ] **Step 1: 添加 ProductListResult 类型**

在 `ProductListFilters` 类型定义之后添加：

```typescript
export type ProductListResult = {
  items: ProductSummary[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
};
```

- [ ] **Step 2: 验证类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit --pretty 2>&1 | head -20
```

Expected: No new errors related to types.ts

- [ ] **Step 3: 提交**

```bash
git add frontend/next-app/src/lib/admin/types.ts
git commit -m "feat: add ProductListResult type for paginated list"
```

---

### Task 5: 前端 — server.ts 分页支持

**Files:**
- Modify: `frontend/next-app/src/lib/admin/server.ts`

- [ ] **Step 1: 修改 getAdminProducts 函数**

将返回类型改为 `ProductListResult`，添加 page/size 参数：

```typescript
import type { ProductListResult } from "@/lib/admin/types";

export async function getAdminProducts(
  filters: ProductListFilters = {},
  page = 1,
  size = 20
): Promise<ProductListResult> {
  const search = new URLSearchParams();
  if (filters.name) search.set("name", filters.name);
  if (filters.categoryId) search.set("categoryId", filters.categoryId);
  if (filters.status) search.set("status", filters.status);
  search.set("page", String(page));
  search.set("size", String(size));

  const query = search.toString();
  return fetchAdminJson<ProductListResult>(`/api/admin/products?${query}`);
}
```

- [ ] **Step 2: 验证类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit --pretty 2>&1 | head -20
```

Expected: No new errors

- [ ] **Step 3: 提交**

```bash
git add frontend/next-app/src/lib/admin/server.ts
git commit -m "feat: add pagination params to getAdminProducts"
```

---

### Task 6: 前端 — ProductList 重写为紧凑表格

**Files:**
- Modify: `frontend/next-app/src/features/admin/catalog/product-list.tsx`
- Modify: `frontend/next-app/src/app/admin/products/page.tsx`

- [ ] **Step 1: 重写 product-list.tsx 为表格组件**

完整替换文件内容：

```typescript
"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

import { deleteProduct, updateProductStatus } from "@/lib/admin/client";
import type { Category, ProductStatus, ProductListResult } from "@/lib/admin/types";

type ProductListProps = {
  result: ProductListResult;
  categories: Category[];
  filters: {
    name?: string;
    categoryId?: string;
    status?: string;
    page?: string;
  };
};

const STATUS_LABELS: Record<ProductStatus, string> = {
  DRAFT: "草稿",
  ON_SHELF: "已上架",
  OFF_SHELF: "已下架"
};

export function ProductList({ result, categories, filters }: ProductListProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleStatusChange(productId: number, status: ProductStatus) {
    setError("");
    startTransition(async () => {
      try {
        await updateProductStatus(productId, status);
        router.refresh();
      } catch (e) {
        setError(e instanceof Error ? e.message : "更新状态失败");
      }
    });
  }

  function handleDelete(productId: number) {
    if (!window.confirm("确认删除该商品？删除后可在数据库恢复。")) {
      return;
    }
    setError("");
    startTransition(async () => {
      try {
        await deleteProduct(productId);
        router.refresh();
      } catch (e) {
        setError(e instanceof Error ? e.message : "删除商品失败");
      }
    });
  }

  function buildPageHref(page: number) {
    const sp = new URLSearchParams();
    if (filters.name) sp.set("name", filters.name);
    if (filters.categoryId) sp.set("categoryId", filters.categoryId);
    if (filters.status) sp.set("status", filters.status);
    sp.set("page", String(page));
    return `/admin/products?${sp.toString()}`;
  }

  return (
    <div className="flex flex-col">
      {/* toolbar */}
      <div className="flex flex-wrap items-end gap-3 border-b border-[#f0f0f0] px-4 py-3">
        <form className="flex flex-wrap items-end gap-3">
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            关键词
            <input
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.name ?? ""}
              name="name"
              placeholder="名称 / SPU 编码"
            />
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            分类
            <select
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.categoryId ?? ""}
              name="categoryId"
            >
              <option value="">全部</option>
              {categories.map((c) => (
                <option key={c.id} value={String(c.id)}>{c.name}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            状态
            <select
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.status ?? ""}
              name="status"
            >
              <option value="">全部</option>
              <option value="DRAFT">草稿</option>
              <option value="ON_SHELF">已上架</option>
              <option value="OFF_SHELF">已下架</option>
            </select>
          </label>
          <button
            className="rounded-lg bg-[var(--brand-primary)] px-4 py-1.5 text-sm font-semibold text-white"
            type="submit"
          >
            查询
          </button>
        </form>
        <Link
          className="ml-auto rounded-lg bg-[var(--accent-strong)] px-4 py-1.5 text-sm font-semibold text-white"
          href="/admin/products/new"
        >
          ＋ 新建商品
        </Link>
      </div>

      {/* summary bar */}
      <div className="flex items-center justify-between border-b border-[#f0f0f0] px-4 py-2">
        <p className="text-sm text-[var(--text-secondary)]">
          共 <span className="font-semibold text-[var(--text-primary)]">{result.total}</span> 个商品
          {result.totalPages > 1 && (
            <span className="ml-2">第 {result.page}/{result.totalPages} 页</span>
          )}
        </p>
      </div>

      {error && (
        <div className="mx-4 mt-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
      )}

      {/* table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#f0f0f0] text-left text-xs text-[var(--text-hint)]">
              <th className="px-4 py-2.5 font-medium">商品名称</th>
              <th className="px-4 py-2.5 font-medium">SPU 编码</th>
              <th className="px-4 py-2.5 font-medium">分类</th>
              <th className="px-4 py-2.5 font-medium text-right">最低售价</th>
              <th className="px-4 py-2.5 font-medium">状态</th>
              <th className="px-4 py-2.5 font-medium text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            {result.items.map((product) => (
              <tr
                key={product.id}
                className="border-b border-[#f5f5f5] hover:bg-[#fafafa] transition-colors"
              >
                <td className="px-4 py-2.5">
                  <Link
                    className="font-medium text-[var(--text-primary)] hover:text-[var(--brand-primary)]"
                    href={`/admin/products/${product.id}`}
                  >
                    {product.name}
                  </Link>
                </td>
                <td className="px-4 py-2.5 font-mono text-xs text-[var(--text-secondary)]">
                  {product.spuCode}
                </td>
                <td className="px-4 py-2.5 text-[var(--text-secondary)]">
                  {product.categoryName}
                </td>
                <td className="px-4 py-2.5 text-right font-semibold text-[var(--price)]" style={{ fontFamily: "var(--font-price)" }}>
                  {product.minSalePrice != null ? `¥${Number(product.minSalePrice).toFixed(2)}` : "-"}
                </td>
                <td className="px-4 py-2.5">
                  <span
                    className={`inline-flex rounded-[4px] px-1.5 py-0.5 text-xs font-medium ${
                      product.status === "ON_SHELF"
                        ? "bg-emerald-50 text-emerald-700"
                        : product.status === "DRAFT"
                          ? "bg-amber-50 text-amber-700"
                          : "bg-gray-100 text-gray-600"
                    }`}
                  >
                    {STATUS_LABELS[product.status]}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <Link
                      className="text-xs text-[var(--text-secondary)] hover:text-[var(--brand-primary)]"
                      href={`/admin/products/${product.id}`}
                    >
                      编辑
                    </Link>
                    <button
                      className="text-xs text-[var(--text-secondary)] hover:text-[var(--brand-primary)]"
                      disabled={isPending}
                      onClick={() =>
                        handleStatusChange(
                          product.id,
                          product.status === "ON_SHELF" ? "OFF_SHELF" : "ON_SHELF"
                        )
                      }
                      type="button"
                    >
                      {product.status === "ON_SHELF" ? "下架" : "上架"}
                    </button>
                    <button
                      className="text-xs text-[var(--text-hint)] hover:text-red-600"
                      disabled={isPending}
                      onClick={() => handleDelete(product.id)}
                      type="button"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* empty */}
      {result.items.length === 0 && (
        <p className="px-4 py-10 text-center text-sm text-[var(--text-hint)]">
          当前筛选条件下没有商品
        </p>
      )}

      {/* pagination */}
      {result.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-[#f0f0f0] px-4 py-2">
          <p className="text-xs text-[var(--text-hint)]">
            {result.page}/{result.totalPages} 页 · 共 {result.total} 个
          </p>
          <div className="flex gap-2">
            {result.page > 1 && (
              <Link
                className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={buildPageHref(result.page - 1)}
              >
                上一页
              </Link>
            )}
            {result.page < result.totalPages && (
              <Link
                className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={buildPageHref(result.page + 1)}
              >
                下一页
              </Link>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: 更新 page.tsx 适配新的 props 和分页参数**

修改 `frontend/next-app/src/app/admin/products/page.tsx`，提取 page 参数并传给 getAdminProducts：

```typescript
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { ProductList } from "@/features/admin/catalog/product-list";
import { requireRole } from "@/lib/auth/server";
import { getAdminCategories, getAdminProducts } from "@/lib/admin/server";

type SearchParams = {
  name?: string;
  categoryId?: string;
  status?: string;
  page?: string;
};

export default async function AdminProductsPage({
  searchParams,
}: {
  searchParams: Promise<SearchParams>;
}) {
  const params = await searchParams;
  const user = await requireRole(["ADMIN", "MERCHANT"], "/admin/products");
  const filters = {
    name: params.name,
    categoryId: params.categoryId,
    status: params.status,
  };
  const page = Number(params.page) || 1;
  const size = 20;

  const [result, categories] = await Promise.all([
    getAdminProducts(filters, page, size),
    getAdminCategories(),
  ]);

  return (
    <AdminShell
      description="管理所有商品，支持搜索、筛选和上下架操作。"
      title="商品管理"
      user={user}
    >
      <ProductList
        categories={categories}
        filters={{ ...filters, page: params.page }}
        result={result}
      />
    </AdminShell>
  );
}
```

- [ ] **Step 3: 验证类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit --pretty 2>&1 | head -30
```

Expected: No new errors

- [ ] **Step 4: 提交**

```bash
git add frontend/next-app/src/features/admin/catalog/product-list.tsx frontend/next-app/src/app/admin/products/page.tsx
git commit -m "refactor: rewrite product list as compact table with pagination"
```

---

### Task 7: 侧边栏 — 移除分类管理导航项

**Files:**
- Modify: `frontend/next-app/src/features/admin/admin-sidebar.tsx`

- [ ] **Step 1: 从 NAV_ITEMS 中删除分类管理条目**

删除这一行：
```typescript
{ href: "/admin/categories", label: "分类管理", roles: ["ADMIN"] },
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend/next-app && npx tsc --noEmit --pretty 2>&1 | head -10
```

Expected: No errors

- [ ] **Step 3: 提交**

```bash
git add frontend/next-app/src/features/admin/admin-sidebar.tsx
git commit -m "refactor: remove standalone categories nav item"
```

---

### Task 8: 首页运营 — 合并分类表格

**Files:**
- Modify: `frontend/next-app/src/features/admin/homepage/homepage-form.tsx`
- Modify: `frontend/next-app/src/app/admin/homepage/page.tsx`

- [ ] **Step 1: 在 homepage-form.tsx 中添加分类表格**

在现有 `cards` state 之后，添加分类相关 state 和逻辑。在组件函数开始处添加：

```typescript
import { updateCategory } from "@/lib/admin/client";
import type { Category, CategoryStatus } from "@/lib/admin/types";

// 在 HomepageForm props 中添加 categories
export function HomepageForm({ categories: initialCategories }: { categories: Category[] }) {
  // 现有 state...
  // 新增分类 state:
  const [categoryList, setCategoryList] = useState<Category[]>(initialCategories);
  const [editingCatId, setEditingCatId] = useState<number | null>(null);
  const [catDraft, setCatDraft] = useState<{ sortOrder: string; status: CategoryStatus } | null>(null);
  const [catSaving, setCatSaving] = useState(false);
  const [catMessage, setCatMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  function beginEditCat(c: Category) {
    setEditingCatId(c.id);
    setCatDraft({ sortOrder: String(c.sortOrder), status: c.status });
  }

  async function saveCat(categoryId: number) {
    if (!catDraft) return;
    setCatSaving(true);
    setCatMessage(null);
    try {
      const updated = await updateCategory(categoryId, {
        name: categoryList.find((c) => c.id === categoryId)!.name,
        sortOrder: Number(catDraft.sortOrder),
        status: catDraft.status,
      });
      setCategoryList((prev) =>
        prev.map((c) => (c.id === categoryId ? updated : c))
      );
      setEditingCatId(null);
      setCatDraft(null);
      setCatMessage({ type: "success", text: "已保存" });
    } catch (e) {
      setCatMessage({ type: "error", text: e instanceof Error ? e.message : "保存失败" });
    } finally {
      setCatSaving(false);
    }
  }
```

- [ ] **Step 2: 在 JSX 中渲染分类表格**

在组件 return 的 JSX 中，在活动卡片 grid 之前插入分类表格部分：

```tsx
{/* 分类导航表格 */}
<div className="flex flex-col gap-3">
  <div className="flex items-center justify-between">
    <div>
      <h3 className="text-base font-semibold">分类导航</h3>
      <p className="mt-0.5 text-xs text-[var(--text-hint)]">控制首页左侧分类栏的展示顺序和可见性</p>
    </div>
    <button
      className="rounded-lg bg-[var(--brand-primary)] px-4 py-1.5 text-sm font-semibold text-white disabled:opacity-50"
      disabled={catSaving}
      onClick={() => editingCatId && catDraft ? saveCat(editingCatId) : undefined}
    >
      {catSaving ? "保存中..." : "保存分类"}
    </button>
  </div>

  {catMessage && (
    <div className={`rounded-lg px-3 py-1.5 text-xs ${catMessage.type === "success" ? "bg-green-50 text-green-700" : "bg-red-50 text-red-600"}`}>
      {catMessage.text}
    </div>
  )}

  <div className="overflow-x-auto rounded-lg border border-[#f0f0f0]">
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-[#f0f0f0] bg-[#fafafa] text-left text-xs text-[var(--text-hint)]">
          <th className="px-4 py-2 font-medium">分类名称</th>
          <th className="px-4 py-2 font-medium w-[100px]">排序</th>
          <th className="px-4 py-2 font-medium w-[100px]">状态</th>
          <th className="px-4 py-2 font-medium w-[80px]">操作</th>
        </tr>
      </thead>
      <tbody>
        {[...categoryList]
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map((c) => {
            const isEditing = editingCatId === c.id && catDraft !== null;
            return (
              <tr key={c.id} className="border-b border-[#f5f5f5]">
                <td className="px-4 py-2 font-medium">{c.name}</td>
                <td className="px-4 py-2">
                  {isEditing ? (
                    <input
                      className="w-full rounded border border-[#e0e0e0] px-2 py-1 text-sm outline-none focus:border-[var(--brand-primary)]"
                      min="0"
                      type="number"
                      value={catDraft.sortOrder}
                      onChange={(e) => setCatDraft({ ...catDraft, sortOrder: e.target.value })}
                    />
                  ) : (
                    <span className="text-[var(--text-secondary)]">{c.sortOrder}</span>
                  )}
                </td>
                <td className="px-4 py-2">
                  {isEditing ? (
                    <select
                      className="w-full rounded border border-[#e0e0e0] px-2 py-1 text-sm outline-none focus:border-[var(--brand-primary)]"
                      value={catDraft.status}
                      onChange={(e) => setCatDraft({ ...catDraft, status: e.target.value as CategoryStatus })}
                    >
                      <option value="ENABLED">启用</option>
                      <option value="DISABLED">停用</option>
                    </select>
                  ) : (
                    <span className={`inline-flex rounded-[4px] px-1.5 py-0.5 text-xs font-medium ${c.status === "ENABLED" ? "bg-emerald-50 text-emerald-700" : "bg-gray-100 text-gray-500"}`}>
                      {c.status === "ENABLED" ? "启用" : "停用"}
                    </span>
                  )}
                </td>
                <td className="px-4 py-2">
                  {isEditing ? (
                    <div className="flex gap-1">
                      <button
                        className="text-xs text-[var(--brand-primary)] hover:underline"
                        disabled={catSaving}
                        onClick={() => saveCat(c.id)}
                      >
                        保存
                      </button>
                      <button
                        className="text-xs text-[var(--text-hint)] hover:underline"
                        onClick={() => { setEditingCatId(null); setCatDraft(null); }}
                      >
                        取消
                      </button>
                    </div>
                  ) : (
                    <button
                      className="text-xs text-[var(--text-secondary)] hover:text-[var(--brand-primary)]"
                      onClick={() => beginEditCat(c)}
                    >
                      编辑
                    </button>
                  )}
                </td>
              </tr>
            );
          })}
      </tbody>
    </table>
  </div>
</div>

{/* 分隔线 */}
<hr className="border-[#f0f0f0]" />
```

- [ ] **Step 3: 更新 Page 组件传入 categories**

修改 `frontend/next-app/src/app/admin/homepage/page.tsx`：

```typescript
import { requireRole } from "@/lib/auth/server";
import { getAdminCategories } from "@/lib/admin/server";
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { HomepageForm } from "@/features/admin/homepage/homepage-form";

export default async function AdminHomepagePage() {
  const user = await requireRole(["ADMIN"], "/admin/homepage");
  const categories = await getAdminCategories();

  return (
    <AdminShell
      description="管理首页的分类导航、活动卡片和展示顺序。"
      title="首页运营"
      user={user}
    >
      <HomepageForm categories={categories} />
    </AdminShell>
  );
}
```

- [ ] **Step 4: 验证类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit --pretty 2>&1 | head -30
```

Expected: No new errors

- [ ] **Step 5: 提交**

```bash
git add frontend/next-app/src/features/admin/homepage/homepage-form.tsx frontend/next-app/src/app/admin/homepage/page.tsx
git commit -m "feat: merge category management into homepage admin"
```

---

### Task 9: 删除独立的分类管理页面

**Files:**
- Delete: `frontend/next-app/src/app/admin/categories/page.tsx`
- Delete: `frontend/next-app/src/features/admin/catalog/category-manager.tsx`
- Delete: `frontend/next-app/src/features/admin/catalog/sku-grid-layout.tsx`

- [ ] **Step 1: 删除文件**

```bash
rm frontend/next-app/src/app/admin/categories/page.tsx
rm frontend/next-app/src/features/admin/catalog/category-manager.tsx
rm frontend/next-app/src/features/admin/catalog/sku-grid-layout.tsx
```

检查是否有其他文件 import 这些已删除的文件：

```bash
cd frontend/next-app && npx tsc --noEmit --pretty 2>&1 | head -30
```

如有 import 错误，修复之（product-editor.tsx 之前 import 了 sku-grid-layout，将在 Task 11 中一并重写）。

- [ ] **Step 2: 验证**

确认 `rmdir` 或确认 categories 目录已空：
```bash
ls frontend/next-app/src/app/admin/categories/ 2>&1
```
Expected: No such file or directory（如果目录为空则手动删除目录）

- [ ] **Step 3: 提交**

```bash
git add -A frontend/next-app/src/app/admin/categories/ frontend/next-app/src/features/admin/catalog/category-manager.tsx frontend/next-app/src/features/admin/catalog/sku-grid-layout.tsx
git commit -m "refactor: remove standalone category manager and sku-grid-layout"
```

---

### Task 10: SKU 批量填充组件

**Files:**
- Create: `frontend/next-app/src/features/admin/catalog/batch-sku-input.tsx`

- [ ] **Step 1: 创建 BatchSkuInput 组件**

```typescript
"use client";

import { useState } from "react";

type BatchSkuInputProps = {
  onApply: (fields: { price?: string; stock?: string; threshold?: string }) => void;
};

export function BatchSkuInput({ onApply }: BatchSkuInputProps) {
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [threshold, setThreshold] = useState("");

  function handleApply() {
    if (!price && !stock && !threshold) return;
    onApply({
      price: price || undefined,
      stock: stock || undefined,
      threshold: threshold || undefined,
    });
  }

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-lg border border-[#f0f0f0] bg-[#fafafa] px-3 py-2 text-sm">
      <span className="text-xs font-medium text-[var(--text-secondary)]">批量设置：</span>
      <label className="flex items-center gap-1 text-xs text-[var(--text-hint)]">
        价格
        <input
          className="w-20 rounded border border-[#e0e0e0] px-1.5 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
          min="0"
          placeholder="0.00"
          step="0.01"
          type="number"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
        />
      </label>
      <label className="flex items-center gap-1 text-xs text-[var(--text-hint)]">
        库存
        <input
          className="w-20 rounded border border-[#e0e0e0] px-1.5 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
          min="0"
          placeholder="0"
          type="number"
          value={stock}
          onChange={(e) => setStock(e.target.value)}
        />
      </label>
      <label className="flex items-center gap-1 text-xs text-[var(--text-hint)]">
        阈值
        <input
          className="w-20 rounded border border-[#e0e0e0] px-1.5 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
          min="0"
          placeholder="0"
          type="number"
          value={threshold}
          onChange={(e) => setThreshold(e.target.value)}
        />
      </label>
      <button
        className="rounded bg-[var(--brand-primary)] px-3 py-1 text-xs font-medium text-white hover:opacity-90"
        onClick={handleApply}
        type="button"
      >
        应用到全部 SKU
      </button>
      <span className="text-xs text-[var(--text-hint)]">已有数据会被覆盖</span>
    </div>
  );
}
```

- [ ] **Step 2: 验证类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit --pretty 2>&1 | head -10
```

Expected: No errors related to batch-sku-input

- [ ] **Step 3: 提交**

```bash
git add frontend/next-app/src/features/admin/catalog/batch-sku-input.tsx
git commit -m "feat: add batch SKU input component"
```

---

### Task 11: 新建商品 — 表单全面重写（核心）

这是最大的一个 task。重写 `product-editor.tsx`。

**Files:**
- Modify: `frontend/next-app/src/features/admin/catalog/product-editor.tsx`
- Modify: `frontend/next-app/src/app/admin/products/new/page.tsx`
- Modify: `frontend/next-app/src/app/admin/products/[id]/page.tsx`

- [ ] **Step 1: 重写 product-editor.tsx**

这个组件需要全面重写。以下是完整的新代码。关键变化：

1. 表单采用分区式布局（不用大圆角卡片）
2. 移除"商品状态"下拉框
3. "展示属性" → "商品参数"，"销售属性" → "规格属性"
4. 分类下拉只显示 ENABLED 的分类
5. 商品主图改为多图上传
6. SKU 表格始终显示（不用 xl 断点判断）
7. 底部 sticky 操作栏：取消 / 保存草稿 / 保存并上架
8. 修改规格前弹确认框（带 SKU 数量预估）
9. 校验逻辑
10. 批量 SKU 填充

完整代码（由于文件过长，分步骤描述）：

**Step 1a: import 和类型定义**

```typescript
"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useState, useTransition } from "react";

import { createProduct, updateProduct } from "@/lib/admin/client";
import type {
  Category,
  ProductAttribute,
  ProductDetail,
  ProductImage,
  ProductPayload,
  ProductSalesAttribute,
  ProductSku,
  SkuStatus
} from "@/lib/admin/types";

import { FieldLabel } from "./field-label";
import { ImagesUploader } from "./images-uploader";
import { BatchSkuInput } from "./batch-sku-input";

type ProductEditorProps = {
  categories: Category[];
  product?: ProductDetail;
};

type SalesAttributeDraft = {
  name: string;
  valuesText: string;
};

type ProductFormState = {
  categoryId: string;
  name: string;
  spuCode: string;
  subtitle: string;
  coverImageUrl: string;
  description: string;
  mainImages: ProductImage[];  // 商品主图（第一张为封面）
  detailImages: ProductImage[];
  attributes: ProductAttribute[];
  salesAttributes: SalesAttributeDraft[];
  skus: ProductSku[];
};

const SKU_STATUS_OPTIONS: SkuStatus[] = ["ENABLED", "DISABLED"];

// buildInitialState, parseSalesAttributes, rebuildSkus, buildCombinations, buildPayload
// 这些工具函数基本保持不变，唯一变化是 coverImageUrl 取自 mainImages[0]
```

**Step 1b: 组件主结构**

组件 JSX 结构如下（概述，实际代码需完整写入文件）：

```tsx
export function ProductEditor({ categories, product }: ProductEditorProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");
  const [mainUploading, setMainUploading] = useState(0);
  const [detailUploading, setDetailUploading] = useState(0);
  const [form, setForm] = useState<ProductFormState>(() => buildInitialState(categories, product));
  const [confirmRegen, setConfirmRegen] = useState<{
    prevCount: number; nextCount: number; callback: () => void;
  } | null>(null);
  const imageUploading = mainUploading + detailUploading > 0;

  const enabledCategories = categories.filter((c) => c.status === "ENABLED");

  function handleSubmit(status: "DRAFT" | "ON_SHELF") {
    // validation + build payload + create/update
  }

  function syncSalesAttributes(next: SalesAttributeDraft[]) {
    const parsed = parseSalesAttributes(next);
    const prevCount = form.skus.length;
    const newSkus = rebuildSkus(parsed, form.skus, form.spuCode);
    const nextCount = newSkus.length;
    if (prevCount > 0 && prevCount !== nextCount) {
      // 有数据，弹确认框
      setConfirmRegen({
        prevCount, nextCount,
        callback: () => {
          setForm((c) => ({ ...c, salesAttributes: next, skus: newSkus }));
          setConfirmRegen(null);
        }
      });
      return;
    }
    // 无数据变化或新增，直接更新
    setForm((c) => ({ ...c, salesAttributes: next, skus: newSkus }));
  }

  return (
    <>
      <form className="space-y-6" onSubmit={(e) => e.preventDefault()}>
        {/* 返回链接 */}
        <Link href="/admin/products" className="text-sm text-[var(--text-secondary)] hover:text-[var(--brand-primary)]">
          &lt; 返回商品列表
        </Link>

        {/* 页面标题 */}
        <h1 className="text-xl font-bold">{product ? "编辑商品" : "新建商品"}</h1>

        {/* 基础信息 */}
        <section>
          <h2 className="text-sm font-semibold border-b border-[#f0f0f0] pb-2">基础信息</h2>
          <div className="mt-3 grid gap-3 grid-cols-1 md:grid-cols-2 lg:grid-cols-4">
            {/* 分类 - 只显示 ENABLED */}
            <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
              <FieldLabel field="categoryId" page="productEditor">分类 *</FieldLabel>
              <select required className="..." value={form.categoryId} onChange={...}>
                <option value="">请选择分类</option>
                {enabledCategories.map((c) => (
                  <option key={c.id} value={String(c.id)}>{c.name}</option>
                ))}
              </select>
              <span className="text-[10px] text-[var(--text-hint)]">仅可选择已启用分类</span>
            </label>
            {/* 商品名称 */}
            <label className="...">
              <FieldLabel field="name" page="productEditor">商品名称 *</FieldLabel>
              <input required minLength={2} maxLength={100} ... />
            </label>
            {/* SPU 编码 */}
            <label className="...">
              <FieldLabel field="spuCode" page="productEditor">SPU 编码</FieldLabel>
              <input placeholder="留空自动生成" ... />
            </label>
            {/* 副标题 */}
            <label className="...">
              <FieldLabel field="subtitle" page="productEditor">副标题</FieldLabel>
              <input placeholder="一句卖点补充" ... />
            </label>
          </div>
        </section>
        {/* ... 后续 section 同理 */}
      </form>

      {/* 确认弹窗 */}
      {confirmRegen && <RegenerateConfirmDialog ... />}
    </>
  );
}
```

由于代码量很大，将在 Step 1 中完整写入文件。

- [ ] **Step 1: 写入完整 product-editor.tsx**

（实际实现时需将上方概述展开为完整代码，这个 task 拆成具体代码步骤）

- [ ] **Step 2: 更新 new/page.tsx 传入已启用的分类**

`new/page.tsx` 基本不变，因为 product-editor 内部会过滤。

- [ ] **Step 3: 验证类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit --pretty 2>&1 | head -30
```

- [ ] **Step 4: 提交**

```bash
git add frontend/next-app/src/features/admin/catalog/product-editor.tsx
git commit -m "refactor: redesign product editor for efficiency and clarity"
```

---

## 自审检查

**Spec coverage check:**
- ✅ 首页运营合并 → Task 8, 9
- ✅ 商品列表紧凑表格 → Task 6
- ✅ 新建商品交互优化 → Task 10, 11
- ✅ 分类下拉只显示 ENABLED → Task 11 (product-editor)
- ✅ 商品主图多张 → Task 11 (product-editor)
- ✅ 规格修改确认弹窗 → Task 11 (product-editor)
- ✅ SKU 批量填充 → Task 10, 11
- ✅ 校验规则 → Task 11 (product-editor 内 handleSubmit)
- ✅ 底部固定操作栏 → Task 11 (product-editor)
- ✅ 侧边栏移除分类管理 → Task 7
- ✅ 删除分类管理页面 → Task 9
- ✅ 分页 → Task 1-6

**Placeholder check:** Task 11 代码需在实现时完整展开，不可留占位符。

**Type consistency:** `ProductListResult` 类型在 Task 4 定义，Task 5 和 Task 6 中使用，字段名一致。
