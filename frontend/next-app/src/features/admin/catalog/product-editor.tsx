"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

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
  mainImages: ProductImage[];
  detailImages: ProductImage[];
  attributes: ProductAttribute[];
  salesAttributes: SalesAttributeDraft[];
  skus: ProductSku[];
};

const SKU_STATUS_OPTIONS: SkuStatus[] = ["ENABLED", "DISABLED"];

export function ProductEditor({ categories, product }: ProductEditorProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");
  const [mainUploading, setMainUploading] = useState(0);
  const [detailUploading, setDetailUploading] = useState(0);
  const [form, setForm] = useState<ProductFormState>(() => buildInitialState(categories, product));
  const [pendingRegen, setPendingRegen] = useState<{
    nextAttrs: SalesAttributeDraft[];
    prevCount: number;
    nextCount: number;
  } | null>(null);
  const imageUploading = mainUploading + detailUploading > 0;

  const enabledCategories = categories.filter((c) => c.status === "ENABLED");

  // ─── submit ─────────────────────────────────────────────
  function handleSubmit(status: "DRAFT" | "ON_SHELF") {
    setError("");
    if (imageUploading) {
      setError("图片正在上传中，请稍后提交");
      return;
    }

    // validation
    if (!form.categoryId) { setError("请选择分类"); return; }
    const trimmedName = form.name.trim();
    if (trimmedName.length < 2 || trimmedName.length > 100) { setError("商品名称需 2–100 字"); return; }
    if (!form.coverImageUrl && form.mainImages.length === 0) { setError("请至少上传一张商品主图"); return; }

    for (const sku of form.skus) {
      const p = Number(sku.price);
      if (isNaN(p) || p < 0) { setError("SKU 价格须 >= 0"); return; }
      const s = Number(sku.stock);
      if (!Number.isInteger(s) || s < 0) { setError("SKU 库存须为非负整数"); return; }
    }

    // duplicate SKU code check
    const codes = form.skus.map((s) => s.skuCode.trim()).filter(Boolean);
    if (new Set(codes).size !== codes.length) { setError("SKU 编码不可重复"); return; }

    startTransition(async () => {
      try {
        const payload = buildPayload(form, status);
        const savedProduct = product
          ? await updateProduct(product.id, payload)
          : await createProduct(payload);
        router.replace(`/admin/products/${savedProduct.id}`);
        router.refresh();
      } catch (e) {
        setError(e instanceof Error ? e.message : "保存失败");
      }
    });
  }

  // ─── sales attr -> SKU rebuild ─────────────────────────
  function syncSalesAttributes(next: SalesAttributeDraft[]) {
    const parsed = parseSalesAttributes(next);
    const prevSkus = form.skus;
    const prevCount = prevSkus.length;
    const newSkus = rebuildSkus(parsed, prevSkus, form.spuCode);
    const nextCount = newSkus.length;

    // no existing data -- just rebuild
    const hasUserData = prevSkus.some(
      (s) => Number(s.price) !== 0 || Number(s.stock) !== 0
    );
    if (!hasUserData || prevCount === nextCount) {
      setForm((c) => ({ ...c, salesAttributes: next, skus: newSkus }));
      return;
    }

    // user has data and counts differ -- show confirm
    setPendingRegen({ nextAttrs: next, prevCount, nextCount });
  }

  function confirmRegen() {
    if (!pendingRegen) return;
    const parsed = parseSalesAttributes(pendingRegen.nextAttrs);
    const newSkus = rebuildSkus(parsed, form.skus, form.spuCode);
    setForm((c) => ({
      ...c,
      salesAttributes: pendingRegen.nextAttrs,
      skus: newSkus,
    }));
    setPendingRegen(null);
  }

  // --- render ---------------------------------------------
  const totalCombinations = parseSalesAttributes(form.salesAttributes).reduce(
    (acc, a) => (a.values.length > 0 ? acc * a.values.length : acc), 1
  );

  return (
    <>
      {/* Regeneration confirm modal */}
      {pendingRegen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="mx-4 w-full max-w-sm rounded-xl bg-white p-6 shadow-2xl">
            <h3 className="text-base font-semibold">修改规格属性将重新生成 SKU</h3>
            <p className="mt-2 text-sm text-[var(--text-secondary)]">
              您已填写的部分 SKU 价格和库存数据可能丢失。
            </p>
            <p className="mt-3 text-sm">
              修改前 <span className="font-semibold">{pendingRegen.prevCount}</span> 个 SKU
              {" -> "}
              修改后 <span className="font-semibold">{pendingRegen.nextCount}</span> 个 SKU
            </p>
            <div className="mt-4 flex gap-3">
              <button
                className="rounded-lg border border-[#e0e0e0] px-4 py-2 text-sm"
                onClick={() => setPendingRegen(null)}
                type="button"
              >
                取消
              </button>
              <button
                className="rounded-lg bg-[var(--brand-primary)] px-4 py-2 text-sm font-semibold text-white"
                onClick={confirmRegen}
                type="button"
              >
                确认重新生成
              </button>
            </div>
          </div>
        </div>
      )}

      <form className="space-y-6" onSubmit={(e) => e.preventDefault()}>
        {/* back link */}
        <Link
          className="text-sm text-[var(--text-secondary)] hover:text-[var(--brand-primary)]"
          href="/admin/products"
        >
          &lt; 返回商品列表
        </Link>

        <h1 className="text-xl font-bold">{product ? "编辑商品" : "新建商品"}</h1>

        {error && (
          <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
        )}

        {/* ---- 基础信息 ---- */}
        <section>
          <h2 className="text-sm font-semibold border-b border-[#f0f0f0] pb-2">基础信息</h2>
          <div className="mt-3 grid gap-3 grid-cols-1 md:grid-cols-2 lg:grid-cols-4">
            {/* category */}
            <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
              <FieldLabel field="categoryId" page="productEditor">分类 *</FieldLabel>
              <select
                className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
                required
                value={form.categoryId}
                onChange={(e) => setForm((c) => ({ ...c, categoryId: e.target.value }))}
              >
                <option value="">请选择分类</option>
                {enabledCategories.map((c) => (
                  <option key={c.id} value={String(c.id)}>{c.name}</option>
                ))}
              </select>
              <span className="text-[10px] text-[var(--text-hint)]">仅可选择已启用分类</span>
            </label>

            {/* name */}
            <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
              <FieldLabel field="name" page="productEditor">商品名称 *</FieldLabel>
              <input
                className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
                maxLength={100}
                minLength={2}
                required
                value={form.name}
                onChange={(e) => setForm((c) => ({ ...c, name: e.target.value }))}
              />
              <span className="text-[10px] text-[var(--text-hint)]">2-100 字</span>
            </label>

            {/* spuCode */}
            <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
              <FieldLabel field="spuCode" page="productEditor">SPU 编码</FieldLabel>
              <input
                className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
                placeholder="留空自动生成"
                value={form.spuCode}
                onChange={(e) => {
                  const spuCode = e.target.value;
                  setForm((c) => ({
                    ...c,
                    spuCode,
                    skus: rebuildSkus(parseSalesAttributes(c.salesAttributes), c.skus, spuCode),
                  }));
                }}
              />
            </label>

            {/* subtitle */}
            <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
              <FieldLabel field="subtitle" page="productEditor">副标题</FieldLabel>
              <input
                className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
                placeholder="一句卖点补充"
                value={form.subtitle}
                onChange={(e) => setForm((c) => ({ ...c, subtitle: e.target.value }))}
              />
            </label>
          </div>
        </section>

        {/* ---- 商品图片 ---- */}
        <section>
          <h2 className="text-sm font-semibold border-b border-[#f0f0f0] pb-2">商品图片</h2>
          <div className="mt-3 grid gap-4 lg:grid-cols-2">
            {/* main images */}
            <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
              <FieldLabel field="coverImageUrl" page="productEditor">商品主图 *</FieldLabel>
              <ImagesUploader
                value={form.mainImages.map((img, i) => ({ url: img.imageUrl, sortOrder: i }))}
                onChange={(images) => {
                  const mapped: ProductImage[] = images.map((img, i) => ({
                    imageUrl: img.url,
                    sortOrder: i,
                  }));
                  setForm((c) => ({
                    ...c,
                    mainImages: mapped,
                    coverImageUrl: mapped[0]?.imageUrl ?? c.coverImageUrl,
                  }));
                }}
                onUploadingChange={setMainUploading}
              />
              <span className="text-[10px] text-[var(--text-hint)]">第一张自动设为封面，建议 800x800</span>
            </label>

            {/* detail images */}
            <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
              <FieldLabel field="detailImageUrl" page="productEditor">商品详情图</FieldLabel>
              <ImagesUploader
                value={form.detailImages
                  .filter((img) => img.imageUrl)
                  .map((img, i) => ({ url: img.imageUrl, sortOrder: i }))}
                onChange={(images) =>
                  setForm((c) => ({
                    ...c,
                    detailImages: images.map((img) => ({
                      imageUrl: img.url,
                      sortOrder: img.sortOrder,
                    })),
                  }))
                }
                onUploadingChange={setDetailUploading}
              />
            </label>
          </div>
        </section>

        {/* ---- 商品描述 ---- */}
        <section>
          <h2 className="text-sm font-semibold border-b border-[#f0f0f0] pb-2">商品描述</h2>
          <div className="mt-3">
            <textarea
              className="min-h-[120px] w-full rounded-lg border border-[#e0e0e0] px-3 py-2 text-sm outline-none focus:border-[var(--brand-primary)]"
              placeholder="支持富文本。详细描述商品特点、规格、使用说明等。"
              value={form.description}
              onChange={(e) => setForm((c) => ({ ...c, description: e.target.value }))}
            />
          </div>
        </section>

        {/* ---- 商品参数（原"展示属性"） ---- */}
        <section>
          <div className="flex items-center justify-between border-b border-[#f0f0f0] pb-2">
            <div>
              <h2 className="text-sm font-semibold">商品参数（选填）</h2>
              <p className="text-[10px] text-[var(--text-hint)]">用于详情页展示，不影响 SKU</p>
            </div>
            <button
              className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs"
              onClick={() =>
                setForm((c) => ({
                  ...c,
                  attributes: [
                    ...c.attributes,
                    { name: "", value: "", sortOrder: c.attributes.length },
                  ],
                }))
              }
              type="button"
            >
              + 添加参数
            </button>
          </div>
          {form.attributes.length > 0 && (
            <div className="mt-2 overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-[var(--text-hint)]">
                    <th className="px-2 py-1.5 font-medium">参数名</th>
                    <th className="px-2 py-1.5 font-medium">参数值</th>
                    <th className="px-2 py-1.5 font-medium w-[80px]">排序</th>
                    <th className="px-2 py-1.5 font-medium w-[60px]"></th>
                  </tr>
                </thead>
                <tbody>
                  {form.attributes.map((attr, i) => (
                    <tr key={`attr-${i}`}>
                      <td className="px-2 py-1">
                        <input
                          className="w-full rounded border border-[#e0e0e0] px-2 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
                          placeholder="参数名"
                          value={attr.name}
                          onChange={(e) =>
                            setForm((c) => ({
                              ...c,
                              attributes: c.attributes.map((a, ai) =>
                                ai === i ? { ...a, name: e.target.value } : a
                              ),
                            }))
                          }
                        />
                      </td>
                      <td className="px-2 py-1">
                        <input
                          className="w-full rounded border border-[#e0e0e0] px-2 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
                          placeholder="参数值"
                          value={attr.value}
                          onChange={(e) =>
                            setForm((c) => ({
                              ...c,
                              attributes: c.attributes.map((a, ai) =>
                                ai === i ? { ...a, value: e.target.value } : a
                              ),
                            }))
                          }
                        />
                      </td>
                      <td className="px-2 py-1">
                        <input
                          className="w-full rounded border border-[#e0e0e0] px-2 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
                          min="0"
                          type="number"
                          value={attr.sortOrder}
                          onChange={(e) =>
                            setForm((c) => ({
                              ...c,
                              attributes: c.attributes.map((a, ai) =>
                                ai === i ? { ...a, sortOrder: Number(e.target.value) } : a
                              ),
                            }))
                          }
                        />
                      </td>
                      <td className="px-2 py-1">
                        <button
                          className="text-xs text-red-500 hover:underline"
                          onClick={() =>
                            setForm((c) => ({
                              ...c,
                              attributes: c.attributes.filter((_, ai) => ai !== i),
                            }))
                          }
                          type="button"
                        >
                          删除
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* ---- 规格属性（原"销售属性"） ---- */}
        <section>
          <div className="flex items-center justify-between border-b border-[#f0f0f0] pb-2">
            <div>
              <h2 className="text-sm font-semibold">规格属性（选填，最多 2 组）</h2>
              <p className="text-[10px] text-[var(--text-hint)]">
                用于生成 SKU。属性值用逗号分隔，如&quot;黑,白,灰&quot;。最多添加 2 组。
              </p>
            </div>
            <button
              className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs disabled:opacity-50"
              disabled={form.salesAttributes.length >= 2}
              onClick={() =>
                syncSalesAttributes([
                  ...form.salesAttributes,
                  { name: "", valuesText: "" },
                ])
              }
              type="button"
            >
              + 添加规格 ({form.salesAttributes.length}/2)
            </button>
          </div>
          {form.salesAttributes.length > 0 && (
            <div className="mt-2 overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-[var(--text-hint)]">
                    <th className="px-2 py-1.5 font-medium">规格名</th>
                    <th className="px-2 py-1.5 font-medium">规格值（逗号分隔）</th>
                    <th className="px-2 py-1.5 font-medium w-[60px]"></th>
                  </tr>
                </thead>
                <tbody>
                  {form.salesAttributes.map((sa, i) => (
                    <tr key={`sa-${i}`}>
                      <td className="px-2 py-1">
                        <input
                          className="w-full rounded border border-[#e0e0e0] px-2 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
                          placeholder="例如 颜色"
                          value={sa.name}
                          onChange={(e) => {
                            const next = form.salesAttributes.map((s, si) =>
                              si === i ? { ...s, name: e.target.value } : s
                            );
                            syncSalesAttributes(next);
                          }}
                        />
                      </td>
                      <td className="px-2 py-1">
                        <input
                          className="w-full rounded border border-[#e0e0e0] px-2 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
                          placeholder="例如 黑, 白, 灰"
                          value={sa.valuesText}
                          onChange={(e) => {
                            const next = form.salesAttributes.map((s, si) =>
                              si === i ? { ...s, valuesText: e.target.value } : s
                            );
                            syncSalesAttributes(next);
                          }}
                        />
                      </td>
                      <td className="px-2 py-1">
                        <button
                          className="text-xs text-red-500 hover:underline"
                          onClick={() =>
                            syncSalesAttributes(
                              form.salesAttributes.filter((_, si) => si !== i)
                            )
                          }
                          type="button"
                        >
                          删除
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {form.salesAttributes.some((s) => s.valuesText.trim()) && (
            <p className="mt-2 text-xs text-[var(--text-secondary)]">
              当前将生成{" "}
              <span className="font-semibold">{totalCombinations}</span>{" "}
              个 SKU
              {totalCombinations > 100 && (
                <span className="ml-1 text-amber-600">
                  SKU 数量较多，建议减少规格值
                </span>
              )}
            </p>
          )}
        </section>

        {/* ---- SKU 列表 ---- */}
        <section>
          <h2 className="text-sm font-semibold border-b border-[#f0f0f0] pb-2">
            SKU 列表（{form.skus.length} 个）
          </h2>

          {form.skus.length > 0 && (
            <>
              <div className="mt-2">
                <BatchSkuInput
                  onApply={({ price, stock, threshold }) => {
                    setForm((c) => ({
                      ...c,
                      skus: c.skus.map((sku) => ({
                        ...sku,
                        ...(price !== undefined ? { price } : {}),
                        ...(stock !== undefined ? { stock: Number(stock) } : {}),
                        ...(threshold !== undefined
                          ? { lowStockThreshold: Number(threshold) }
                          : {}),
                      })),
                    }));
                  }}
                />
              </div>
              <div className="mt-2 overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs text-[var(--text-hint)]">
                      <th className="px-2 py-1.5 font-medium">规格</th>
                      <th className="px-2 py-1.5 font-medium">SKU 编码</th>
                      <th className="px-2 py-1.5 font-medium text-right">价格 *</th>
                      <th className="px-2 py-1.5 font-medium text-right">库存</th>
                      <th className="px-2 py-1.5 font-medium text-right">低库存阈值</th>
                      <th className="px-2 py-1.5 font-medium">状态</th>
                    </tr>
                  </thead>
                  <tbody>
                    {form.skus.map((sku, i) => (
                      <tr key={sku.salesAttrValueKey || `sku-${i}`} className="border-b border-[#f5f5f5]">
                        <td className="px-2 py-1 text-xs font-medium">
                          {sku.salesAttrValueText || "默认"}
                        </td>
                        <td className="px-2 py-1">
                          <input
                            className="w-full rounded border border-[#e0e0e0] px-1.5 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
                            placeholder="留空自动生成"
                            value={sku.skuCode}
                            onChange={(e) =>
                              setForm((c) => ({
                                ...c,
                                skus: c.skus.map((s, si) =>
                                  si === i ? { ...s, skuCode: e.target.value } : s
                                ),
                              }))
                            }
                          />
                        </td>
                        <td className="px-2 py-1">
                          <input
                            className="w-24 rounded border border-[#e0e0e0] px-1.5 py-1 text-xs text-right outline-none focus:border-[var(--brand-primary)]"
                            min="0"
                            step="0.01"
                            type="number"
                            value={sku.price}
                            onChange={(e) =>
                              setForm((c) => ({
                                ...c,
                                skus: c.skus.map((s, si) =>
                                  si === i ? { ...s, price: e.target.value } : s
                                ),
                              }))
                            }
                          />
                        </td>
                        <td className="px-2 py-1">
                          <input
                            className="w-20 rounded border border-[#e0e0e0] px-1.5 py-1 text-xs text-right outline-none focus:border-[var(--brand-primary)]"
                            min="0"
                            type="number"
                            value={sku.stock}
                            onChange={(e) =>
                              setForm((c) => ({
                                ...c,
                                skus: c.skus.map((s, si) =>
                                  si === i ? { ...s, stock: Number(e.target.value) } : s
                                ),
                              }))
                            }
                          />
                        </td>
                        <td className="px-2 py-1">
                          <input
                            className="w-20 rounded border border-[#e0e0e0] px-1.5 py-1 text-xs text-right outline-none focus:border-[var(--brand-primary)]"
                            min="0"
                            type="number"
                            value={sku.lowStockThreshold}
                            onChange={(e) =>
                              setForm((c) => ({
                                ...c,
                                skus: c.skus.map((s, si) =>
                                  si === i ? { ...s, lowStockThreshold: Number(e.target.value) } : s
                                ),
                              }))
                            }
                          />
                        </td>
                        <td className="px-2 py-1">
                          <select
                            className="rounded border border-[#e0e0e0] px-1.5 py-1 text-xs outline-none focus:border-[var(--brand-primary)]"
                            value={sku.status}
                            onChange={(e) =>
                              setForm((c) => ({
                                ...c,
                                skus: c.skus.map((s, si) =>
                                  si === i
                                    ? { ...s, status: e.target.value as SkuStatus }
                                    : s
                                ),
                              }))
                            }
                          >
                            {SKU_STATUS_OPTIONS.map((s) => (
                              <option key={s} value={s}>{s === "ENABLED" ? "启用" : "停用"}</option>
                            ))}
                          </select>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}

          {form.skus.length === 0 && (
            <p className="mt-2 rounded-lg border border-dashed border-[#e0e0e0] px-4 py-6 text-center text-xs text-[var(--text-hint)]">
              先配置规格属性。若无规格，可直接保存一个默认 SKU。
            </p>
          )}
        </section>

        {/* ---- sticky bottom bar ---- */}
        <div className="sticky bottom-0 -mx-2 border-t border-[#f0f0f0] bg-white px-2 py-3 mt-6 flex items-center justify-end gap-3">
          <Link
            className="rounded-lg border border-[#e0e0e0] px-5 py-2 text-sm"
            href="/admin/products"
          >
            取消
          </Link>
          <button
            className="rounded-lg border border-[#e0e0e0] px-5 py-2 text-sm"
            disabled={isPending || imageUploading}
            onClick={() => handleSubmit("DRAFT")}
            type="button"
          >
            {isPending ? "保存中..." : "保存草稿"}
          </button>
          <button
            className="rounded-lg bg-[var(--brand-primary)] px-5 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
            disabled={isPending || imageUploading}
            onClick={() => handleSubmit("ON_SHELF")}
            type="button"
          >
            {isPending ? "保存中..." : "保存并上架"}
          </button>
        </div>
      </form>
    </>
  );
}

// --- helpers ------------------------------------------------

function buildInitialState(categories: Category[], product?: ProductDetail): ProductFormState {
  if (product) {
    return {
      categoryId: String(product.categoryId),
      name: product.name,
      spuCode: product.spuCode,
      subtitle: product.subtitle ?? "",
      coverImageUrl: product.coverImageUrl ?? "",
      description: product.description ?? "",
      mainImages: product.coverImageUrl
        ? [{ imageUrl: product.coverImageUrl, sortOrder: 0 }]
        : [],
      detailImages: product.detailImages ?? [],
      attributes: product.attributes ?? [],
      salesAttributes: product.salesAttributes.map((sa) => ({
        name: sa.name,
        valuesText: sa.values.map((v) => v.value).join(", "),
      })),
      skus: product.skus.length > 0
        ? product.skus.map((sku) => ({ ...sku, price: String(sku.price) }))
        : rebuildSkus([], [], product.spuCode),
    };
  }
  return {
    categoryId: categories[0] ? String(categories[0].id) : "",
    name: "",
    spuCode: "",
    subtitle: "",
    coverImageUrl: "",
    description: "",
    mainImages: [],
    detailImages: [],
    attributes: [],
    salesAttributes: [],
    skus: rebuildSkus([], [], ""),
  };
}

function parseSalesAttributes(drafts: SalesAttributeDraft[]): ProductSalesAttribute[] {
  return drafts
    .map((draft, index) => {
      const values = draft.valuesText
        .split(",")
        .map((v) => v.trim())
        .filter(Boolean)
        .map((v, vi) => ({ value: v, sortOrder: vi }));
      return { name: draft.name.trim(), sortOrder: index, values };
    })
    .filter((item) => item.name && item.values.length > 0);
}

function rebuildSkus(
  salesAttributes: ProductSalesAttribute[],
  currentSkus: ProductSku[],
  spuCode: string
): ProductSku[] {
  const currentSkuMap = new Map(currentSkus.map((s) => [s.salesAttrValueKey, s]));
  const combinations = buildCombinations(salesAttributes);

  if (combinations.length === 0) {
    const existingDefault = currentSkuMap.get("default");
    return [
      existingDefault ?? {
        skuCode: spuCode ? `${spuCode}-001` : "",
        salesAttrValueKey: "default",
        salesAttrValueText: "默认",
        price: "0",
        stock: 0,
        lowStockThreshold: 0,
        status: "ENABLED",
      },
    ];
  }

  return combinations.map((combo, index) => {
    const existing = currentSkuMap.get(combo.key);
    return (
      existing ?? {
        skuCode: spuCode
          ? `${spuCode}-${String(index + 1).padStart(3, "0")}`
          : "",
        salesAttrValueKey: combo.key,
        salesAttrValueText: combo.text,
        price: "0",
        stock: 0,
        lowStockThreshold: 0,
        status: "ENABLED",
      }
    );
  });
}

function buildCombinations(
  salesAttributes: ProductSalesAttribute[]
): Array<{ key: string; text: string }> {
  if (salesAttributes.length === 0) return [];

  let combos: Array<{ key: string; text: string }> = [{ key: "", text: "" }];
  for (const sa of salesAttributes) {
    const next: Array<{ key: string; text: string }> = [];
    for (const c of combos) {
      for (const v of sa.values) {
        const kp = `${sa.name}:${v.value}`;
        const tp = `${sa.name} / ${v.value}`;
        next.push({
          key: c.key ? `${c.key}|${kp}` : kp,
          text: c.text ? `${c.text} . ${tp}` : tp,
        });
      }
    }
    combos = next;
  }
  return combos;
}

function buildPayload(form: ProductFormState, status: string): ProductPayload {
  const salesAttributes = parseSalesAttributes(form.salesAttributes);
  const skus = rebuildSkus(salesAttributes, form.skus, form.spuCode);

  return {
    categoryId: Number(form.categoryId),
    name: form.name.trim(),
    spuCode: form.spuCode.trim(),
    subtitle: form.subtitle.trim(),
    coverImageUrl: form.coverImageUrl.trim(),
    description: form.description,
    status: status as ProductPayload["status"],
    detailImages: form.detailImages
      .filter((img) => img.imageUrl.trim())
      .map((img, i) => ({
        imageUrl: img.imageUrl.trim(),
        sortOrder: Number.isFinite(img.sortOrder) ? img.sortOrder : i,
      })),
    attributes: form.attributes
      .filter((a) => a.name.trim() && a.value.trim())
      .map((a, i) => ({
        name: a.name.trim(),
        value: a.value.trim(),
        sortOrder: Number.isFinite(a.sortOrder) ? a.sortOrder : i,
      })),
    salesAttributes,
    skus: skus.map((sku) => ({
      ...sku,
      skuCode: sku.skuCode.trim(),
      price: sku.price.trim(),
    })),
  };
}
