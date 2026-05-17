// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ImageUploader } from "./image-uploader";

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

describe("ImageUploader", () => {
  it("renders empty state with upload prompt", () => {
    render(<ImageUploader value="" onChange={() => {}} />);

    expect(screen.getByText("上传封面图")).toBeDefined();
    expect(screen.getByText("建议 800x800")).toBeDefined();
  });

  it("renders thumbnail when value is provided", () => {
    render(<ImageUploader value="https://example.com/img.jpg" onChange={() => {}} />);

    const img = screen.getByAltText("封面图");
    expect(img).toBeDefined();
    expect(img.getAttribute("src")).toContain("x-oss-process=image/resize");
  });

  it("calls onChange with empty string on remove", () => {
    const onChange = vi.fn();
    render(<ImageUploader value="https://example.com/img.jpg" onChange={onChange} />);

    fireEvent.click(screen.getByLabelText("删除封面图"));

    expect(onChange).toHaveBeenCalledWith("");
  });

  it("reports uploading count while uploading", async () => {
    const onChange = vi.fn();
    const onUploadingChange = vi.fn();
    render(<ImageUploader value="" onChange={onChange} onUploadingChange={onUploadingChange} />);
    const input = screen.getByLabelText("选择封面图") as HTMLInputElement;
    const file = new File(["image"], "cover.jpg", { type: "image/jpeg" });

    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => expect(onChange).toHaveBeenCalledWith("https://example.com/img.jpg"));
    expect(onUploadingChange).toHaveBeenCalledWith(1);
    expect(onUploadingChange).toHaveBeenLastCalledWith(0);
  });
});
