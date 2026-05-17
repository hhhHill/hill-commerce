export type OssStsToken = {
  accessKey: string;
  secretKey: string;
  securityToken: string;
  ossRegion: string;
  bucket: string;
  endpoint: string;
  uploadDir: string;
};

export type UploadState = "empty" | "compressing" | "uploading" | "uploaded" | "error";

export type UploadResult = {
  url: string;
  key: string;
};

export type ImageUploadMeta = {
  url: string;
  sortOrder: number;
};
