"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useState, useTransition } from "react";

import { createCategory, deleteCategory, updateCategory } from "@/lib/admin/client";
import type { Category, CategoryStatus } from "@/lib/admin/types";

type CategoryManagerProps = {
  categories: Category[];
};

type CategoryFormState = {
  name: string;
  sortOrder: string;
  status: CategoryStatus;
};

const EMPTY_FORM: CategoryFormState = {
  name: "",
  sortOrder: "0",
  status: "ENABLED"
};

export function CategoryManager({ categories }: CategoryManagerProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [createForm, setCreateForm] = useState<CategoryFormState>(EMPTY_FORM);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [drafts, setDrafts] = useState<Record<number, CategoryFormState>>({});
  const [error, setError] = useState("");

  function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    startTransition(async () => {
      try {
        await createCategory({
          name: createForm.name.trim(),
          sortOrder: Number(createForm.sortOrder),
          status: createForm.status
        });
        setCreateForm(EMPTY_FORM);
        router.refresh();
      } catch (submitError) {
        setError(submitError instanceof Error ? submitError.message : "保存分类失败");
      }
    });
  }

  function beginEdit(category: Category) {
    setEditingId(category.id);
    setDrafts((current) => ({
      ...current,
      [category.id]: {
        name: category.name,
        sortOrder: String(category.sortOrder),
        status: category.status
      }
    }));
  }

  function handleUpdate(categoryId: number) {
    const draft = drafts[categoryId];
    if (!draft) {
      return;
    }

    setError("");
    startTransition(async () => {
      try {
        await updateCategory(categoryId, {
          name: draft.name.trim(),
          sortOrder: Number(draft.sortOrder),
          status: draft.status
        });
        setEditingId(null);
        router.refresh();
      } catch (submitError) {
        setError(submitError instanceof Error ? submitError.message : "更新分类失败");
      }
    });
  }

  function handleDelete(categoryId: number) {
    if (!window.confirm("删除后无法恢复，确认继续吗？")) {
      return;
    }

    setError("");
    startTransition(async () => {
      try {
        await deleteCategory(categoryId);
        router.refresh();
      } catch (submitError) {
        setError(submitError instanceof Error ? submitError.message : "删除分类失败");
      }
    });
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[360px_minmax(0,1fr)]">
      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="space-y-2">
          <h2 className="text-2xl font-semibold">新建分类</h2>
          <p className="text-sm leading-6 text-black/65">仅支持一级分类，字段包括名称、排序和启用状态。</p>
        </div>
        <form className="mt-5 flex flex-col gap-4" onSubmit={handleCreate}>
          <label className="flex flex-col gap-2 text-sm font-medium">
            分类名称
            <input
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              value={createForm.name}
              onChange={(event) => setCreateForm((current) => ({ ...current, name: event.target.value }))}
              required
            />
          </label>
          <label className="flex flex-col gap-2 text-sm font-medium">
            排序
            <input
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              min="0"
              type="number"
              value={createForm.sortOrder}
              onChange={(event) => setCreateForm((current) => ({ ...current, sortOrder: event.target.value }))}
              required
            />
          </label>
          <label className="flex flex-col gap-2 text-sm font-medium">
            状态
            <select
              className="rounded-2xl border border-black/10 bg-[#fffaf5] px-4 py-3 outline-none transition focus:border-[var(--accent)]"
              value={createForm.status}
              onChange={(event) =>
                setCreateForm((current) => ({ ...current, status: event.target.value as CategoryStatus }))
              }
            >
              <option value="ENABLED">启用</option>
              <option value="DISABLED">停用</option>
            </select>
          </label>
          {error ? <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p> : null}
          <button
            className="rounded-2xl bg-[var(--accent)] px-5 py-3 font-semibold text-white transition hover:bg-[var(--accent-strong)] disabled:opacity-60"
            disabled={isPending}
            type="submit"
          >
            {isPending ? "提交中..." : "创建分类"}
          </button>
        </form>
      </section>

      <section className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-semibold">分类列表</h2>
            <p className="mt-2 text-sm text-black/65">默认按排序和创建顺序展示，停用后不能用于新建商品。</p>
          </div>
          <span className="rounded-full bg-[#f4e0cc] px-3 py-1 text-sm font-medium text-[var(--accent-strong)]">
            共 {categories.length} 个
          </span>
        </div>
        <div className="mt-5 space-y-4">
          {categories.map((category) => {
            const draft = drafts[category.id];
            const isEditing = editingId === category.id && draft;

            return (
              <article
                key={category.id}
                className="rounded-[24px] border border-black/10 bg-[#fffaf5] p-5 shadow-[0_10px_26px_rgba(29,20,13,0.04)]"
              >
                {isEditing ? (
                  <div className="grid gap-3 md:grid-cols-3">
                    <input
                      className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                      value={draft.name}
                      onChange={(event) =>
                        setDrafts((current) => ({
                          ...current,
                          [category.id]: { ...draft, name: event.target.value }
                        }))
                      }
                    />
                    <input
                      className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                      min="0"
                      type="number"
                      value={draft.sortOrder}
                      onChange={(event) =>
                        setDrafts((current) => ({
                          ...current,
                          [category.id]: { ...draft, sortOrder: event.target.value }
                        }))
                      }
                    />
                    <select
                      className="rounded-2xl border border-black/10 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                      value={draft.status}
                      onChange={(event) =>
                        setDrafts((current) => ({
                          ...current,
                          [category.id]: { ...draft, status: event.target.value as CategoryStatus }
                        }))
                      }
                    >
                      <option value="ENABLED">启用</option>
                      <option value="DISABLED">停用</option>
                    </select>
                  </div>
                ) : (
                  <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                    <div>
                      <h3 className="text-lg font-semibold">{category.name}</h3>
                      <p className="mt-1 text-sm text-black/60">排序 {category.sortOrder}</p>
                    </div>
                    <span className="w-fit rounded-full bg-white px-3 py-1 text-sm font-medium text-black/70">
                      {category.status === "ENABLED" ? "启用中" : "已停用"}
                    </span>
                  </div>
                )}
                <div className="mt-4 flex flex-wrap gap-3">
                  {isEditing ? (
                    <>
                      <button
                        className="rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
                        disabled={isPending}
                        onClick={() => handleUpdate(category.id)}
                        type="button"
                      >
                        保存修改
                      </button>
                      <button
                        className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium"
                        disabled={isPending}
                        onClick={() => setEditingId(null)}
                        type="button"
                      >
                        取消
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium"
                        disabled={isPending}
                        onClick={() => beginEdit(category)}
                        type="button"
                      >
                        编辑
                      </button>
                      <button
                        className="rounded-full border border-red-200 px-4 py-2 text-sm font-medium text-red-700"
                        disabled={isPending}
                        onClick={() => handleDelete(category.id)}
                        type="button"
                      >
                        删除
                      </button>
                    </>
                  )}
                </div>
              </article>
            );
          })}
          {categories.length === 0 ? (
            <p className="rounded-[24px] border border-dashed border-black/10 px-5 py-10 text-center text-sm text-black/55">
              还没有分类，先在左侧创建第一个一级分类。
            </p>
          ) : null}
        </div>
      </section>
    </div>
  );
}
