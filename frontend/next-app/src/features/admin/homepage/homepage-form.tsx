"use client";

import { useCallback, useEffect, useState } from "react";
import Image from "next/image";
import {
  batchUpdateActivityCards,
  listActivityCards,
  type ActivityCard
} from "@/lib/admin/homepage";
import { compressImage } from "@/lib/upload/image-compress";
import { uploadImage } from "@/lib/upload/oss-client";

export function HomepageForm() {
  const [cards, setCards] = useState<ActivityCard[]>([]);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [imageFiles, setImageFiles] = useState<Map<number, File>>(new Map());

  const fetchCards = useCallback(async () => {
    try {
      const items = await listActivityCards();
      setCards(items);
    } catch {
      setMessage({ type: "error", text: "加载活动卡片失败" });
    }
  }, []);

  useEffect(() => {
    fetchCards();
  }, [fetchCards]);

  function updateCard(id: number, field: string, value: string | boolean | number) {
    setCards((prev) =>
      prev.map((c) => (c.id === id ? { ...c, [field]: value } : c))
    );
  }

  function handleImageSelect(id: number, file: File) {
    setImageFiles((prev) => {
      const next = new Map(prev);
      next.set(id, file);
      return next;
    });
    updateCard(id, "imageUrl", URL.createObjectURL(file));
  }

  async function uploadPendingImage(file: File): Promise<string> {
    const compressed = await compressImage(file, { maxSize: 10 });
    const result = await uploadImage(compressed, file.name, "homepage");
    return result.url;
  }

  async function handleSave() {
    setSaving(true);
    setMessage(null);

    try {
      // Upload any pending images
      const cardsWithUploadedImages = await Promise.all(
        cards.map(async (card) => {
          const pendingFile = imageFiles.get(card.id);
          if (pendingFile && card.imageUrl?.startsWith("blob:")) {
            const uploadedUrl = await uploadPendingImage(pendingFile);
            return { ...card, imageUrl: uploadedUrl };
          }
          return card;
        })
      );

      await batchUpdateActivityCards({
        cards: cardsWithUploadedImages.map((c) => ({
          id: c.id,
          title: c.title,
          imageUrl: c.imageUrl,
          linkUrl: c.linkUrl,
          isActive: c.isActive,
          sortOrder: c.sortOrder
        }))
      });

      setImageFiles(new Map());
      setMessage({ type: "success", text: "保存成功" });
      await fetchCards();
    } catch (e) {
      setMessage({ type: "error", text: e instanceof Error ? e.message : "保存失败" });
    } finally {
      setSaving(false);
    }
  }

  if (cards.length === 0) {
    return <div className="py-8 text-center text-sm text-[var(--text-hint)]">加载中...</div>;
  }

  return (
    <div className="flex flex-col gap-6">
      {message && (
        <div
          className={`rounded-[2px] px-4 py-2 text-sm ${
            message.type === "success"
              ? "bg-green-50 text-green-700"
              : "bg-red-50 text-red-600"
          }`}
        >
          {message.text}
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        {cards.map((card) => (
          <div
            className="flex flex-col gap-3 rounded-[2px] border border-[#f0f0f0] bg-white p-4"
            key={card.id}
          >
            {/* Image preview / upload */}
            <div className="relative flex aspect-[2/1] items-center justify-center overflow-hidden rounded-[2px] border border-[#f0f0f0] bg-[#fafafa]">
              {card.imageUrl ? (
                <Image
                  alt={card.title}
                  className="object-cover"
                  fill
                  sizes="(max-width: 768px) 50vw, 25vw"
                  src={card.imageUrl}
                />
              ) : (
                <span className="text-sm text-[var(--text-hint)]">暂无图片</span>
              )}
              <label className="absolute bottom-2 right-2 cursor-pointer rounded-[2px] bg-black/50 px-2 py-1 text-xs text-white hover:bg-black/70">
                更换
                <input
                  accept="image/*"
                  className="hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) handleImageSelect(card.id, file);
                  }}
                  type="file"
                />
              </label>
            </div>

            {/* Title */}
            <label className="flex flex-col gap-1">
              <span className="text-xs text-[var(--text-secondary)]">标题</span>
              <input
                className="rounded-[2px] border border-[#e0e0e0] px-3 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
                maxLength={100}
                onChange={(e) => updateCard(card.id, "title", e.target.value)}
                type="text"
                value={card.title}
              />
            </label>

            {/* Link */}
            <label className="flex flex-col gap-1">
              <span className="text-xs text-[var(--text-secondary)]">跳转链接</span>
              <input
                className="rounded-[2px] border border-[#e0e0e0] px-3 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
                maxLength={500}
                onChange={(e) => updateCard(card.id, "linkUrl", e.target.value)}
                type="text"
                value={card.linkUrl}
              />
            </label>

            {/* Active toggle */}
            <label className="flex items-center gap-2">
              <input
                checked={card.isActive}
                onChange={(e) => updateCard(card.id, "isActive", e.target.checked)}
                type="checkbox"
              />
              <span className="text-xs text-[var(--text-secondary)]">启用（前台可见）</span>
            </label>
          </div>
        ))}
      </div>

      <div className="flex items-center gap-3">
        <button
          className="rounded-[2px] bg-[var(--brand-primary)] px-6 py-2 text-sm text-white hover:bg-[var(--brand-primary-hover)] disabled:opacity-50"
          disabled={saving}
          onClick={handleSave}
        >
          {saving ? "保存中..." : "保存设置"}
        </button>
        {message && message.type === "success" && (
          <span className="text-xs text-green-600">已保存，刷新首页即可生效</span>
        )}
      </div>
    </div>
  );
}
