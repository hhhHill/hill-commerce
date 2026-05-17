// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ImagesUploader } from "./images-uploader";
import { uploadToOss } from "@/lib/upload/oss-client";

vi.mock("@/lib/upload/image-compress", () => ({
  compressImage: vi.fn().mockResolvedValue(new Blob(["test"], { type: "image/jpeg" }))
}));

vi.mock("@/lib/upload/oss-client", () => ({
  requestStsToken: vi.fn().mockResolvedValue({
    accessKey: "ak",
    secretKey: "sk",
    securityToken: "st",
    ossRegion: "oss-r",
    bucket: "b",
    endpoint: "e",
    uploadDir: "p/"
  }),
  uploadToOss: vi.fn().mockResolvedValue("https://example.com/img.jpg")
}));

afterEach(() => {
  cleanup();
});

function createMockFileList(files: File[]): FileList {
  const mock: Record<number, File> = {};
  for (let i = 0; i < files.length; i++) {
    mock[i] = files[i];
  }
  return { ...mock, length: files.length, item: (i: number) => files[i] ?? null } as unknown as FileList;
}

const hasText = (text: string) => (content: string) => content.includes(text);

describe("ImagesUploader", () => {
  it("renders add button when empty", () => {
    render(<ImagesUploader value={[]} onChange={() => {}} />);
    expect(screen.getByText(hasText("共 0 张"))).toBeDefined();
  });

  it("renders thumbnails for provided images", () => {
    const images = [
      { url: "https://example.com/a.jpg", sortOrder: 0 },
      { url: "https://example.com/b.jpg", sortOrder: 1 }
    ];

    render(<ImagesUploader value={images} onChange={() => {}} />);

    expect(screen.getByText(hasText("共 2 张"))).toBeDefined();
    expect(screen.getByText(hasText("拖拽调整顺序"))).toBeDefined();
    expect(screen.getAllByAltText(/详情图/)).toHaveLength(2);
  });

  it("calls onChange without removed image on delete", () => {
    const onChange = vi.fn();
    const images = [
      { url: "https://example.com/a.jpg", sortOrder: 0 },
      { url: "https://example.com/b.jpg", sortOrder: 1 }
    ];

    render(<ImagesUploader value={images} onChange={onChange} />);
    fireEvent.click(screen.getByLabelText("删除详情图 1"));

    expect(onChange).toHaveBeenCalledWith([{ url: "https://example.com/b.jpg", sortOrder: 0 }]);
  });

  it("updates sortOrder after drag sorting", () => {
    const onChange = vi.fn();
    const images = [
      { url: "https://example.com/a.jpg", sortOrder: 0 },
      { url: "https://example.com/b.jpg", sortOrder: 1 }
    ];

    render(<ImagesUploader value={images} onChange={onChange} />);
    const thumbnails = screen.getAllByAltText(/详情图/);

    fireEvent.dragStart(thumbnails[0]);
    fireEvent.drop(thumbnails[1]);

    expect(onChange).toHaveBeenCalledWith([
      { url: "https://example.com/b.jpg", sortOrder: 0 },
      { url: "https://example.com/a.jpg", sortOrder: 1 }
    ]);
  });

  it("shows failed upload summary when upload errors occur", async () => {
    uploadToOss.mockRejectedValueOnce(new Error("模拟上传失败"));
    const onChange = vi.fn();
    const file = new File(["image"], "fail.jpg", { type: "image/jpeg" });

    render(<ImagesUploader value={[]} onChange={onChange} />);
    const input = screen.getByLabelText("选择详情图") as HTMLInputElement;
    fireEvent.change(input, { target: { files: createMockFileList([file]) } });

    await waitFor(() => expect(screen.getByText(hasText("上传失败"))).toBeDefined());
  });

  it("disables add button while uploading and shows count", async () => {
    const onChange = vi.fn();
    const onUploadingChange = vi.fn();
    const file = new File(["image"], "test.jpg", { type: "image/jpeg" });

    render(<ImagesUploader value={[]} onChange={onChange} onUploadingChange={onUploadingChange} />);
    const input = screen.getByLabelText("选择详情图") as HTMLInputElement;
    fireEvent.change(input, { target: { files: createMockFileList([file]) } });

    await waitFor(() => expect(onChange).toHaveBeenCalled());
    expect(onUploadingChange).toHaveBeenCalledWith(1);
    expect(onUploadingChange).toHaveBeenLastCalledWith(0);
  });
});
