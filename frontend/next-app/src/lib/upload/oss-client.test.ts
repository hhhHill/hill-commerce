import { describe, expect, it, vi } from "vitest";

import { uploadImage } from "./oss-client";

describe("uploadImage", () => {
  it("sends multipart request and returns upload result", async () => {
    const responseBody = { url: "https://example.com/img.jpg", key: "uploads/products/123_test.jpg" };
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(responseBody), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );

    const result = await uploadImage(
      new Blob(["image"], { type: "image/jpeg" }),
      "test.jpg",
      "products"
    );

    expect(result.url).toBe("https://example.com/img.jpg");
    expect(result.key).toBe("uploads/products/123_test.jpg");
    expect(global.fetch).toHaveBeenCalledWith("/api/admin/oss/upload", {
      method: "POST",
      body: expect.any(FormData),
      credentials: "include"
    });
  });

  it("throws error message from response body on failure", async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ message: "Category must not be blank" }), { status: 400 })
    );

    await expect(uploadImage(new Blob(["image"]), "test.jpg", "")).rejects.toThrow(
      "Category must not be blank"
    );
  });

  it("throws fallback error when response body is not json", async () => {
    global.fetch = vi.fn().mockResolvedValue(new Response("Internal Server Error", { status: 500 }));

    await expect(uploadImage(new Blob(["image"]), "test.jpg", "products")).rejects.toThrow(
      "上传失败，请重试"
    );
  });
});
