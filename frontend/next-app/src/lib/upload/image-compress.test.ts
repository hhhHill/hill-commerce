// @vitest-environment jsdom

import { beforeEach, describe, expect, it, vi } from "vitest";

import { compressImage } from "./image-compress";

describe("compressImage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    URL.createObjectURL = vi.fn(() => "blob:test");
    URL.revokeObjectURL = vi.fn();
    const createElement = document.createElement.bind(document);
    vi.spyOn(document, "createElement").mockImplementation((tagName: string) => {
      if (tagName !== "canvas") {
        return createElement(tagName);
      }
      return {
        width: 0,
        height: 0,
        getContext: vi.fn(() => ({ drawImage: vi.fn() })),
        toBlob: vi.fn((callback: BlobCallback, type?: string) => {
          callback(new Blob(["compressed"], { type: type ?? "image/jpeg" }));
        })
      } as unknown as HTMLCanvasElement;
    });
    vi.stubGlobal(
      "Image",
      class {
        width = 2400;
        height = 1200;
        onload: (() => void) | null = null;
        onerror: (() => void) | null = null;

        set src(_value: string) {
          queueMicrotask(() => this.onload?.());
        }
      }
    );
  });

  it("rejects files larger than maxSize", async () => {
    const largeFile = new File(["x".repeat(11 * 1024 * 1024)], "large.jpg", {
      type: "image/jpeg"
    });

    await expect(compressImage(largeFile, { maxSize: 10 })).rejects.toThrow("文件大小超过限制");
  });

  it("rejects unsupported file types", async () => {
    const gifFile = new File(["fake-gif"], "test.gif", { type: "image/gif" });

    await expect(compressImage(gifFile)).rejects.toThrow("不支持的图片格式");
  });

  it("compresses image and keeps the configured mime type", async () => {
    const file = new File(["fake-jpeg"], "test.jpg", { type: "image/jpeg" });

    const result = await compressImage(file, { maxWidth: 1920, quality: 0.8 });

    expect(result.type).toBe("image/jpeg");
    expect(result.size).toBeGreaterThan(0);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:test");
  });
});
