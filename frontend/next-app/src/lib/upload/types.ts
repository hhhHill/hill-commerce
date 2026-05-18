export type OssUploadResult = {
  url: string;
  key: string;
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
