"use client";

import { useRef, useState } from "react";

import { compressImage } from "@/lib/upload/image-compress";
import { uploadImage } from "@/lib/upload/oss-client";
import type { ImageUploadMeta } from "@/lib/upload/types";

type ImagesUploaderProps = {
  value: ImageUploadMeta[];
  onChange: (images: ImageUploadMeta[]) => void;
  onUploadingChange?: (uploadingCount: number) => void;
  maxCount?: number;
};

type FailedFile = {
  slotId: number;
  fileName: string;
  error: string;
};

export function ImagesUploader({ value, onChange, onUploadingChange, maxCount = 10 }: ImagesUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const dragIndexRef = useRef<number | null>(null);
  const slotIdRef = useRef(0);
  const [uploadingCount, setUploadingCount] = useState(0);
  const [failedFiles, setFailedFiles] = useState<FailedFile[]>([]);

  async function handleFiles(files: FileList) {
    const remaining = maxCount - value.length;
    if (remaining <= 0) {
      return;
    }

    const filesToUpload = Array.from(files).slice(0, remaining);
    setUploadingCount(filesToUpload.length);
    onUploadingChange?.(filesToUpload.length);
    setFailedFiles([]);

    const uploaded: ImageUploadMeta[] = [];
    const newFailed: FailedFile[] = [];

    for (const file of filesToUpload) {
      const slotId = slotIdRef.current++;
      try {
        const compressed = await compressImage(file);
        const result = await uploadImage(compressed, file.name, "products");
        uploaded.push({ url: result.url, sortOrder: 0 });
      } catch (error) {
        newFailed.push({
          slotId,
          fileName: file.name,
          error: error instanceof Error ? error.message : "上传失败"
        });
      }
    }

    if (uploaded.length > 0) {
      const base = value.length;
      onChange([...value, ...uploaded.map((image, index) => ({ url: image.url, sortOrder: base + index }))]);
    }

    if (newFailed.length > 0) {
      setFailedFiles(newFailed);
    }

    setUploadingCount(0);
    onUploadingChange?.(0);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  function removeImage(index: number) {
    onChange(value.filter((_, itemIndex) => itemIndex !== index).map((image, itemIndex) => ({ ...image, sortOrder: itemIndex })));
  }

  function handleDrop(targetIndex: number) {
    const sourceIndex = dragIndexRef.current;
    dragIndexRef.current = null;
    if (sourceIndex === null || sourceIndex === targetIndex) {
      return;
    }

    const next = [...value];
    const [moved] = next.splice(sourceIndex, 1);
    next.splice(targetIndex, 0, moved);
    onChange(next.map((image, index) => ({ ...image, sortOrder: index })));
  }

  const isUploading = uploadingCount > 0;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        {value.map((image, index) => (
          <div
            key={`${image.url}-${index}`}
            className="relative h-20 w-20"
            draggable
            onDragStart={() => {
              dragIndexRef.current = index;
            }}
            onDragOver={(event) => event.preventDefault()}
            onDragEnd={() => {
              dragIndexRef.current = null;
            }}
            onDrop={() => handleDrop(index)}
          >
            <img
              src={`${image.url}?x-oss-process=image/resize,w_80,h_80,m_fill`}
              alt={`详情图 ${index + 1}`}
              className="h-20 w-20 cursor-grab rounded-md object-cover"
            />
            <span className="absolute bottom-1 left-1 rounded bg-black/65 px-1 text-[10px] text-white">{index + 1}</span>
            <button
              type="button"
              aria-label={`删除详情图 ${index + 1}`}
              className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-600 text-xs text-white"
              onClick={() => removeImage(index)}
            >
              x
            </button>
          </div>
        ))}

        {value.length < maxCount ? (
          <button
            type="button"
            className="flex h-20 w-20 items-center justify-center rounded-md border-2 border-dashed border-black/15 bg-[#fffaf5] text-2xl text-black/45 hover:border-[var(--accent)] disabled:cursor-not-allowed disabled:opacity-60"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
          >
            {isUploading ? <span className="text-xs">上传中...</span> : "+"}
          </button>
        ) : null}
      </div>

      <input
        ref={fileInputRef}
        aria-label="选择详情图"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        multiple
        className="hidden"
        onChange={(event) => {
          if (event.target.files) {
            void handleFiles(event.target.files);
          }
        }}
      />

      <p className="text-xs text-black/55">
        共 {value.length} 张{value.length > 0 ? "，拖拽调整顺序" : ""}
        {isUploading ? ` · ${uploadingCount} 张上传中...` : ""}
      </p>

      {failedFiles.length > 0 ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
          {failedFiles.length} 张上传失败：
          {failedFiles.map((f) => `${f.fileName}（${f.error}）`).join("、")}
        </div>
      ) : null}
    </div>
  );
}
