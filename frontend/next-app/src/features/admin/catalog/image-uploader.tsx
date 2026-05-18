"use client";

import { useRef, useState } from "react";

import { compressImage } from "@/lib/upload/image-compress";
import { uploadImage } from "@/lib/upload/oss-client";
import type { UploadState } from "@/lib/upload/types";

type ImageUploaderProps = {
  value: string;
  onChange: (url: string) => void;
  onUploadingChange?: (uploadingCount: number) => void;
  placeholder?: string;
  suggestSize?: string;
  maxSize?: number;
};

export function ImageUploader({
  value,
  onChange,
  onUploadingChange,
  placeholder = "上传封面图",
  suggestSize = "建议 800x800",
  maxSize = 10
}: ImageUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const lastFileRef = useRef<File | null>(null);
  const [state, setState] = useState<UploadState>(value ? "uploaded" : "empty");
  const [progress, setProgress] = useState(0);
  const [errorMsg, setErrorMsg] = useState("");

  async function handleFile(file: File) {
    lastFileRef.current = file;
    setErrorMsg("");
    onUploadingChange?.(1);

    try {
      setState("compressing");
      const compressed = await compressImage(file, { maxSize });

      setState("uploading");
      setProgress(20);

      const result = await uploadImage(compressed, file.name, "products");
      setProgress(100);
      setState("uploaded");
      onChange(result.url);
    } catch (error) {
      setErrorMsg(error instanceof Error ? error.message : "上传失败");
      setState("error");
    } finally {
      onUploadingChange?.(0);
    }
  }

  function handleClick() {
    fileInputRef.current?.click();
  }

  function handleRemove() {
    setState("empty");
    setProgress(0);
    setErrorMsg("");
    lastFileRef.current = null;
    onChange("");
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  function handleRetry() {
    if (lastFileRef.current) {
      void handleFile(lastFileRef.current);
    }
  }

  function handleDrop(event: React.DragEvent<HTMLDivElement>) {
    event.preventDefault();
    const file = event.dataTransfer.files[0];
    if (file) {
      void handleFile(file);
    }
  }

  return (
    <div className="flex flex-wrap items-start gap-3">
      <input
        ref={fileInputRef}
        aria-label="选择封面图"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="hidden"
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) {
            void handleFile(file);
          }
        }}
      />

      {state === "uploaded" && value ? (
        <div className="relative h-[120px] w-[120px]">
          <img
            src={`${value}?x-oss-process=image/resize,w_200,h_200,m_fill`}
            alt="封面图"
            className="h-[120px] w-[120px] rounded-lg object-cover"
          />
          <button
            type="button"
            aria-label="删除封面图"
            className="absolute right-1 top-1 flex h-6 w-6 items-center justify-center rounded-full bg-red-600 text-sm text-white"
            onClick={handleRemove}
          >
            x
          </button>
        </div>
      ) : (
        <div
          role="button"
          tabIndex={0}
          className={`flex h-[120px] w-[120px] cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed text-center transition ${
            state === "error" ? "border-red-300 bg-red-50" : "border-black/15 bg-[#fffaf5] hover:border-[var(--accent)]"
          }`}
          onClick={handleClick}
          onDragOver={(event) => event.preventDefault()}
          onDrop={handleDrop}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              handleClick();
            }
          }}
        >
          {state === "compressing" || state === "uploading" ? (
            <div className="w-full px-3">
              <p className="text-xs font-medium text-black/70">
                {state === "compressing" ? "压缩中..." : `上传中 ${progress}%`}
              </p>
              {state === "uploading" ? (
                <div className="mt-2 h-1 rounded-full bg-black/10">
                  <div className="h-1 rounded-full bg-[var(--accent)]" style={{ width: `${progress}%` }} />
                </div>
              ) : null}
            </div>
          ) : state === "error" ? (
            <div className="px-2 text-xs text-red-700">
              <p>{errorMsg}</p>
              <button type="button" className="mt-1 underline" onClick={handleRetry}>
                点击重试
              </button>
            </div>
          ) : (
            <>
              <span className="text-2xl leading-none">+</span>
              <span className="mt-2 text-xs text-black/70">{placeholder}</span>
              <span className="text-[10px] text-black/45">{suggestSize}</span>
            </>
          )}
        </div>
      )}

      <div className="pt-1 text-xs leading-relaxed text-black/55">
        {state === "uploaded" ? (
          <button type="button" className="font-medium text-[var(--accent)] underline" onClick={handleClick}>
            更换图片
          </button>
        ) : (
          <>
            或拖拽图片到此处
            <br />
            支持 jpg / png / webp
            <br />
            最大 {maxSize}MB，自动压缩
          </>
        )}
      </div>
    </div>
  );
}
