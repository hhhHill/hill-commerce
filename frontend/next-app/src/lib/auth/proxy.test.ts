import { beforeEach, describe, expect, it, vi } from "vitest";

import { proxyBackendRequest } from "./proxy";

vi.mock("@/lib/config", () => ({
  getBackendBaseUrl: () => "http://backend.local"
}));

describe("proxyBackendRequest", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("serializes JSON bodies and forwards cookies", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { "content-type": "application/json" }
      })
    );
    global.fetch = fetchMock;

    await proxyBackendRequest({
      method: "POST",
      pathname: "/api/cart",
      cookieHeader: "JSESSIONID=session-id",
      body: { productId: 1, quantity: 2 }
    });

    expect(fetchMock).toHaveBeenCalledWith("http://backend.local/api/cart", {
      method: "POST",
      headers: {
        cookie: "JSESSIONID=session-id",
        "content-type": "application/json"
      },
      body: JSON.stringify({ productId: 1, quantity: 2 }),
      cache: "no-store",
      redirect: "manual"
    });
  });

  it("forwards FormData without forcing JSON content type", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ url: "https://example.com/a.jpg" }), {
        status: 200,
        headers: { "content-type": "application/json" }
      })
    );
    global.fetch = fetchMock;
    const formData = new FormData();
    formData.append("file", new Blob(["image"], { type: "image/jpeg" }), "test.jpg");
    formData.append("category", "products");

    await proxyBackendRequest({
      method: "POST",
      pathname: "/api/admin/oss/upload",
      cookieHeader: "JSESSIONID=session-id",
      body: formData
    });

    expect(fetchMock).toHaveBeenCalledWith("http://backend.local/api/admin/oss/upload", {
      method: "POST",
      headers: {
        cookie: "JSESSIONID=session-id"
      },
      body: formData,
      cache: "no-store",
      redirect: "manual"
    });
  });
});
