import OSS from "ali-oss";

import type { OssStsToken } from "./types";

let cachedToken: { token: OssStsToken; expiresAt: number } | null = null;

export function clearStsCache(): void {
  cachedToken = null;
}

export async function requestStsToken(): Promise<OssStsToken> {
  if (cachedToken && Date.now() < cachedToken.expiresAt) {
    return cachedToken.token;
  }

  const response = await fetch("/api/admin/oss/sts", {
    credentials: "include"
  });

  if (!response.ok) {
    throw new Error("获取上传凭证失败，请刷新重试");
  }

  const token = (await response.json()) as OssStsToken;
  cachedToken = { token, expiresAt: Date.now() + 55 * 60 * 1000 };
  return token;
}

export async function uploadToOss(blob: Blob, fileName: string, token: OssStsToken): Promise<string> {
  const client = new OSS({
    region: token.ossRegion,
    endpoint: token.endpoint,
    accessKeyId: token.accessKey,
    accessKeySecret: token.secretKey,
    stsToken: token.securityToken,
    bucket: token.bucket
  });

  const objectKey = `${normalizeUploadDir(token.uploadDir)}${Date.now()}_${randomSuffix()}_${sanitizeFileName(fileName)}`;

  try {
    const result = await client.put(objectKey, blob, { timeout: 30_000 });
    return result.url;
  } catch (error) {
    throw new Error(`上传失败，请重试: ${error instanceof Error ? error.message : "未知错误"}`);
  }
}

function normalizeUploadDir(uploadDir: string): string {
  return uploadDir.endsWith("/") ? uploadDir : `${uploadDir}/`;
}

function randomSuffix(): string {
  return Math.random().toString(36).slice(2, 8);
}

function sanitizeFileName(fileName: string): string {
  return fileName.replace(/[^a-zA-Z0-9._-]/g, "_");
}
