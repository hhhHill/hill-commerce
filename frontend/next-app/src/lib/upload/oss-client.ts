import type { OssUploadResult } from "./types";

export async function uploadImage(blob: Blob, fileName: string, category: string): Promise<OssUploadResult> {
  const formData = new FormData();
  formData.append("file", blob, fileName);
  formData.append("category", category);

  const response = await fetch("/api/admin/oss/upload", {
    method: "POST",
    body: formData,
    credentials: "include"
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const message = body?.message ?? "上传失败，请重试";
    throw new Error(message);
  }

  return response.json() as Promise<OssUploadResult>;
}
