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
  ProductStatus,
  SkuStatus
} from "@/lib/admin/types";

import { FieldLabel } from "./field-label";
import { FieldHelpRowHeadings } from "./field-help-row-headings";
import { SkuGridLayout, SKU_GRID_CLASS } from "./sku-grid-layout";

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
  status: ProductStatus;
  detailImages: ProductImage[];
  attributes: ProductAttribute[];
  salesAttributes: SalesAttributeDraft[];
  skus: ProductSku[];
};

const PRODUCT_STATUS_OPTIONS: ProductStatus[] = ["DRAFT", "ON_SHELF", "OFF_SHELF"];
const SKU_STATUS_OPTIONS: SkuStatus[] = ["ENABLED", "DISABLED"];

export function ProductEditor({ categories, product }: ProductEditorProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");
  const [form, setForm] = useState<ProductFormState>(() => buildInitialState(categories, product));

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    startTransition(async () => {
      try {
        const payload = buildPayload(form);
        const savedProduct = product
          ? await updateProduct(product.id, payload)
          : await createProduct(payload);
        router.replace(`/admin/products/${savedProduct.id}`);
        router.refresh();
      } catch (submitError) {
        setError(submitError instanceof Error ? submitError.message : "保存商品失败");
      }
    });
  }

  function syncSalesAttributes(nextSalesAttributes: SalesAttributeDraft[]) {
    const parsedSalesAttributes = parseSalesAttributes(nextSalesAttributes);
    setForm((current) => ({
      ...current,
      salesAttributes: nextSalesAttributes,
      skus: rebuildSkus(parsedSalesAttributes, current.skus, current.spuCode)
    }));
  }

  return (
    <form className="space-y-6" onSubmit={handleSubmit}>
      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="space-y-2">
          <h2 className="text-2xl font-semibold">基础信息</h2>
          <p className="text-sm text-black/65">维护分类、名称、SPU 编码、副标题和商品状态。</p>
        </div>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          <label className="flex flex-col gap-2 text-sm font-medium">
            <FieldLabel field="categoryId" page="productEditor">分类</FieldLabel>
            <select
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              required
              value={form.categoryId}
              onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value }))}
            >
              <option value="">请选择分类</option>
              {categories.map((category) => (
                <option
                  key={category.id}
                  disabled={category.status === "DISABLED" && String(category.id) !== form.categoryId}
                  value={String(category.id)}
                >
                  {category.name} {category.status === "DISABLED" ? "（已停用）" : ""}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-2 text-sm font-medium">
            <FieldLabel field="status" page="productEditor">商品状态</FieldLabel>
            <select
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              value={form.status}
              onChange={(event) => setForm((current) => ({ ...current, status: event.target.value as ProductStatus }))}
            >
              {PRODUCT_STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-2 text-sm font-medium">
            <FieldLabel field="name" page="productEditor">商品名称</FieldLabel>
            <input
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              required
              value={form.name}
              onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
            />
          </label>
          <label className="flex flex-col gap-2 text-sm font-medium">
            <FieldLabel field="spuCode" page="productEditor">SPU 编码</FieldLabel>
            <input
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              required
              value={form.spuCode}
              onChange={(event) => {
                const spuCode = event.target.value;
                setForm((current) => ({
                  ...current,
                  spuCode,
                  skus: rebuildSkus(parseSalesAttributes(current.salesAttributes), current.skus, spuCode)
                }));
              }}
            />
          </label>
          <label className="flex flex-col gap-2 text-sm font-medium md:col-span-2">
            <FieldLabel field="subtitle" page="productEditor">副标题</FieldLabel>
            <input
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              value={form.subtitle}
              onChange={(event) => setForm((current) => ({ ...current, subtitle: event.target.value }))}
            />
          </label>
        </div>
      </section>

      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="space-y-2">
          <h2 className="text-2xl font-semibold">图片与描述</h2>
          <p className="text-sm text-black/65">封面图和详情图都使用 URL，描述先存富文本源字符串。</p>
        </div>
        <div className="mt-5 grid gap-4">
          <label className="flex flex-col gap-2 text-sm font-medium">
            <FieldLabel field="coverImageUrl" page="productEditor">封面图 URL</FieldLabel>
            <input
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              value={form.coverImageUrl}
              onChange={(event) => setForm((current) => ({ ...current, coverImageUrl: event.target.value }))}
            />
          </label>
          <label className="flex flex-col gap-2 text-sm font-medium">
            <FieldLabel field="description" page="productEditor">描述源字符串</FieldLabel>
            <textarea
              className="min-h-[180px] rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              value={form.description}
              onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
            />
          </label>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">详情图 URL 列表</h3>
              <button
                className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium"
                onClick={() =>
                  setForm((current) => ({
                    ...current,
                    detailImages: [
                      ...current.detailImages,
                      { imageUrl: "", sortOrder: current.detailImages.length }
                    ]
                  }))
                }
                type="button"
              >
                添加详情图
              </button>
            </div>
            <div className="space-y-3">
              <FieldHelpRowHeadings
                items={[
                  { field: "detailImageUrl", label: "详情图 URL" },
                  { field: "detailImageSortOrder", label: "详情图排序" }
                ]}
                page="productEditor"
              />
              {form.detailImages.map((image, index) => (
                <div key={`detail-image-${index}`} className="grid gap-3 md:grid-cols-[minmax(0,1fr)_120px_auto]">
                  <input
                    className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="详情图 URL"
                    placeholder="详情图 URL，例如 https://example.com/detail.jpg"
                    value={image.imageUrl}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        detailImages: current.detailImages.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, imageUrl: event.target.value } : item
                        )
                      }))
                    }
                  />
                  <input
                    className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="详情图排序"
                    min="0"
                    type="number"
                    value={image.sortOrder}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        detailImages: current.detailImages.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, sortOrder: Number(event.target.value) } : item
                        )
                      }))
                    }
                  />
                  <button
                    className="rounded-full border border-red-200 px-4 py-2 text-sm font-medium text-red-700"
                    onClick={() =>
                      setForm((current) => ({
                        ...current,
                        detailImages: current.detailImages.filter((_, itemIndex) => itemIndex !== index)
                      }))
                    }
                    type="button"
                  >
                    删除
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="space-y-2">
          <h2 className="text-2xl font-semibold">展示属性与销售属性</h2>
          <p className="text-sm text-black/65">展示属性用键值对维护，销售属性最多 2 组，用逗号拆分属性值。</p>
        </div>
        <div className="mt-5 space-y-6">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">展示属性</h3>
              <button
                className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium"
                onClick={() =>
                  setForm((current) => ({
                    ...current,
                    attributes: [...current.attributes, { name: "", value: "", sortOrder: current.attributes.length }]
                  }))
                }
                type="button"
              >
                添加属性
              </button>
            </div>
            <div className="space-y-3">
              <FieldHelpRowHeadings
                items={[
                  { field: "attributeName", label: "展示属性名" },
                  { field: "attributeValue", label: "展示属性值" },
                  { field: "attributeSortOrder", label: "展示属性排序" }
                ]}
                page="productEditor"
              />
              {form.attributes.map((attribute, index) => (
                <div key={`attribute-${index}`} className="grid gap-3 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_120px_auto]">
                  <input
                    className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="展示属性名"
                    placeholder="展示属性名"
                    value={attribute.name}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        attributes: current.attributes.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, name: event.target.value } : item
                        )
                      }))
                    }
                  />
                  <input
                    className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="展示属性值"
                    placeholder="展示属性值"
                    value={attribute.value}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        attributes: current.attributes.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, value: event.target.value } : item
                        )
                      }))
                    }
                  />
                  <input
                    className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="展示属性排序"
                    min="0"
                    type="number"
                    value={attribute.sortOrder}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        attributes: current.attributes.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, sortOrder: Number(event.target.value) } : item
                        )
                      }))
                    }
                  />
                  <button
                    className="rounded-full border border-red-200 px-4 py-2 text-sm font-medium text-red-700"
                    onClick={() =>
                      setForm((current) => ({
                        ...current,
                        attributes: current.attributes.filter((_, itemIndex) => itemIndex !== index)
                      }))
                    }
                    type="button"
                  >
                    删除
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">销售属性</h3>
              <button
                className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium disabled:opacity-50"
                disabled={form.salesAttributes.length >= 2}
                onClick={() => syncSalesAttributes([...form.salesAttributes, { name: "", valuesText: "" }])}
                type="button"
              >
                添加销售属性
              </button>
            </div>
            <div className="space-y-3">
              <FieldHelpRowHeadings
                items={[
                  { field: "salesAttributeName", label: "销售属性名" },
                  { field: "salesAttributeValues", label: "销售属性值" }
                ]}
                page="productEditor"
              />
              {form.salesAttributes.map((salesAttribute, index) => (
                <div key={`sales-attribute-${index}`} className="grid gap-3 rounded-[24px] border border-black/10 bg-[#fffaf5] p-4 md:grid-cols-[220px_minmax(0,1fr)_auto]">
                  <input
                    className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="销售属性名"
                    placeholder="销售属性名，例如 颜色"
                    value={salesAttribute.name}
                    onChange={(event) => {
                      const nextSalesAttributes = form.salesAttributes.map((item, itemIndex) =>
                        itemIndex === index ? { ...item, name: event.target.value } : item
                      );
                      syncSalesAttributes(nextSalesAttributes);
                    }}
                  />
                  <input
                    className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="销售属性值"
                    placeholder="销售属性值，例如 黑, 白, 灰"
                    value={salesAttribute.valuesText}
                    onChange={(event) => {
                      const nextSalesAttributes = form.salesAttributes.map((item, itemIndex) =>
                        itemIndex === index ? { ...item, valuesText: event.target.value } : item
                      );
                      syncSalesAttributes(nextSalesAttributes);
                    }}
                  />
                  <button
                    className="rounded-full border border-red-200 px-4 py-2 text-sm font-medium text-red-700"
                    onClick={() =>
                      syncSalesAttributes(form.salesAttributes.filter((_, itemIndex) => itemIndex !== index))
                    }
                    type="button"
                  >
                    删除
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="space-y-2">
          <h2 className="text-2xl font-semibold">SKU 列表</h2>
          <p className="text-sm text-black/65">销售属性变化会自动重建 SKU 草稿表；编码可手工改写，留空则后端补码。</p>
        </div>
        <div className="mt-5">
          <SkuGridLayout
            header={
              <>
                <div>
                  <FieldLabel field="salesAttributeValues" page="productEditor">组合</FieldLabel>
                </div>
                <div>
                  <FieldLabel field="skuCode" page="productEditor">SKU 编码</FieldLabel>
                </div>
                <div>
                  <FieldLabel field="skuPrice" page="productEditor">SKU 价格</FieldLabel>
                </div>
                <div>
                  <FieldLabel field="skuStock" page="productEditor">SKU 库存</FieldLabel>
                </div>
                <div>
                  <FieldLabel field="skuLowStockThreshold" page="productEditor">低库存阈值</FieldLabel>
                </div>
                <div>
                  <FieldLabel field="skuStatus" page="productEditor">SKU 状态</FieldLabel>
                </div>
              </>
            }
            rows={form.skus.map((sku, index) => (
              <article key={sku.salesAttrValueKey || `sku-${index}`} className="rounded-[24px] border border-black/10 bg-[#fffaf5] p-4">
                <div className={SKU_GRID_CLASS}>
                  <div>
                    <p className="text-xs uppercase tracking-[0.18em] text-black/45 xl:hidden">组合</p>
                    <p className="mt-2 text-sm font-semibold xl:mt-0">{sku.salesAttrValueText || "默认 SKU"}</p>
                  </div>
                  <input
                    className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="SKU 编码"
                    placeholder="SKU 编码，可留空"
                    value={sku.skuCode}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        skus: current.skus.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, skuCode: event.target.value } : item
                        )
                      }))
                    }
                  />
                  <input
                    className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="SKU 价格"
                    min="0"
                    placeholder="价格"
                    step="0.01"
                    type="number"
                    value={sku.price}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        skus: current.skus.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, price: event.target.value } : item
                        )
                      }))
                    }
                  />
                  <input
                    className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="SKU 库存"
                    min="0"
                    placeholder="库存"
                    type="number"
                    value={sku.stock}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        skus: current.skus.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, stock: Number(event.target.value) } : item
                        )
                      }))
                    }
                  />
                  <input
                    className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="SKU 低库存阈值"
                    min="0"
                    placeholder="低库存阈值"
                    type="number"
                    value={sku.lowStockThreshold}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        skus: current.skus.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, lowStockThreshold: Number(event.target.value) } : item
                        )
                      }))
                    }
                  />
                  <select
                    className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    aria-label="SKU 状态"
                    value={sku.status}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        skus: current.skus.map((item, itemIndex) =>
                          itemIndex === index ? { ...item, status: event.target.value as SkuStatus } : item
                        )
                      }))
                    }
                  >
                    {SKU_STATUS_OPTIONS.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                </div>
              </article>
            ))}
          />
          {form.skus.length === 0 ? (
            <p className="rounded-[24px] border border-dashed border-black/10 px-5 py-10 text-center text-sm text-black/55">
              先配置销售属性。若不需要销售属性，也可以直接保存一个默认 SKU 组合。
            </p>
          ) : null}
        </div>
      </section>

      {error ? <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p> : null}

      <div className="flex flex-wrap gap-3">
        <button
          className="rounded-2xl bg-[var(--accent)] px-6 py-3 font-semibold text-white transition hover:bg-[var(--accent-strong)] disabled:opacity-60"
          disabled={isPending}
          type="submit"
        >
          {isPending ? "保存中..." : product ? "更新商品" : "创建商品"}
        </button>
        <button
          className="rounded-2xl border border-black/10 px-6 py-3 font-medium"
          disabled={isPending}
          onClick={() => router.push("/admin/products")}
          type="button"
        >
          返回列表
        </button>
      </div>
    </form>
  );
}

function buildInitialState(categories: Category[], product?: ProductDetail): ProductFormState {
  if (product) {
    return {
      categoryId: String(product.categoryId),
      name: product.name,
      spuCode: product.spuCode,
      subtitle: product.subtitle ?? "",
      coverImageUrl: product.coverImageUrl ?? "",
      description: product.description ?? "",
      status: product.status,
      detailImages: product.detailImages.length > 0 ? product.detailImages : [],
      attributes: product.attributes.length > 0 ? product.attributes : [],
      salesAttributes: product.salesAttributes.map((salesAttribute) => ({
        name: salesAttribute.name,
        valuesText: salesAttribute.values.map((value) => value.value).join(", ")
      })),
      skus: product.skus.length > 0 ? product.skus.map((sku) => ({
        ...sku,
        price: String(sku.price)
      })) : rebuildSkus([], [], product.spuCode)
    };
  }

  return {
    categoryId: categories[0] ? String(categories[0].id) : "",
    name: "",
    spuCode: "",
    subtitle: "",
    coverImageUrl: "",
    description: "",
    status: "DRAFT",
    detailImages: [],
    attributes: [],
    salesAttributes: [],
    skus: rebuildSkus([], [], "")
  };
}

function parseSalesAttributes(drafts: SalesAttributeDraft[]): ProductSalesAttribute[] {
  return drafts
    .map((draft, index) => {
      const values = draft.valuesText
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean)
        .map((value, valueIndex) => ({
          value,
          sortOrder: valueIndex
        }));

      return {
        name: draft.name.trim(),
        sortOrder: index,
        values
      };
    })
    .filter((item) => item.name && item.values.length > 0);
}

function rebuildSkus(salesAttributes: ProductSalesAttribute[], currentSkus: ProductSku[], spuCode: string): ProductSku[] {
  const currentSkuMap = new Map(currentSkus.map((sku) => [sku.salesAttrValueKey, sku]));
  const combinations = buildCombinations(salesAttributes);

  if (combinations.length === 0) {
    const existingDefaultSku = currentSkuMap.get("default");
    return [
      existingDefaultSku ?? {
        skuCode: spuCode ? `${spuCode}-001` : "",
        salesAttrValueKey: "default",
        salesAttrValueText: "默认 SKU",
        price: "0",
        stock: 0,
        lowStockThreshold: 0,
        status: "ENABLED"
      }
    ];
  }

  return combinations.map((combination, index) => {
    const existingSku = currentSkuMap.get(combination.key);
    return (
      existingSku ?? {
        skuCode: spuCode ? `${spuCode}-${String(index + 1).padStart(3, "0")}` : "",
        salesAttrValueKey: combination.key,
        salesAttrValueText: combination.text,
        price: "0",
        stock: 0,
        lowStockThreshold: 0,
        status: "ENABLED"
      }
    );
  });
}

function buildCombinations(salesAttributes: ProductSalesAttribute[]): Array<{ key: string; text: string }> {
  if (salesAttributes.length === 0) {
    return [];
  }

  let combinations: Array<{ key: string; text: string }> = [{ key: "", text: "" }];

  for (const salesAttribute of salesAttributes) {
    const nextCombinations: Array<{ key: string; text: string }> = [];

    for (const combination of combinations) {
      for (const value of salesAttribute.values) {
        const keyPart = `${salesAttribute.name}:${value.value}`;
        const textPart = `${salesAttribute.name} / ${value.value}`;
        nextCombinations.push({
          key: combination.key ? `${combination.key}|${keyPart}` : keyPart,
          text: combination.text ? `${combination.text} · ${textPart}` : textPart
        });
      }
    }

    combinations = nextCombinations;
  }

  return combinations;
}

function buildPayload(form: ProductFormState): ProductPayload {
  const salesAttributes = parseSalesAttributes(form.salesAttributes);
  const skus = rebuildSkus(salesAttributes, form.skus, form.spuCode);

  return {
    categoryId: Number(form.categoryId),
    name: form.name.trim(),
    spuCode: form.spuCode.trim(),
    subtitle: form.subtitle.trim(),
    coverImageUrl: form.coverImageUrl.trim(),
    description: form.description,
    status: form.status,
    detailImages: form.detailImages
      .filter((image) => image.imageUrl.trim())
      .map((image, index) => ({
        imageUrl: image.imageUrl.trim(),
        sortOrder: Number.isFinite(image.sortOrder) ? image.sortOrder : index
      })),
    attributes: form.attributes
      .filter((attribute) => attribute.name.trim() && attribute.value.trim())
      .map((attribute, index) => ({
        name: attribute.name.trim(),
        value: attribute.value.trim(),
        sortOrder: Number.isFinite(attribute.sortOrder) ? attribute.sortOrder : index
      })),
    salesAttributes,
    skus: skus.map((sku) => ({
      ...sku,
      skuCode: sku.skuCode.trim(),
      price: sku.price.trim()
    }))
  };
}
