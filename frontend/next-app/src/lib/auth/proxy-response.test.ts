import { describe, it, expect } from "vitest";

import { getProxyResponseBody } from "./proxy-response";

describe("getProxyResponseBody", () => {
  it("omits response body for 204 responses", () => {
    const body = getProxyResponseBody(204, "");
    const response = new Response(body, { status: 204 });

    expect(body).toBe(null);
    expect(response.status).toBe(204);
  });

  it("preserves response body for non-empty non-204 responses", async () => {
    const body = getProxyResponseBody(200, "{\"ok\":true}");
    const response = new Response(body, {
      status: 200,
      headers: { "content-type": "application/json" }
    });

    expect(body).toBe("{\"ok\":true}");
    expect(await response.text()).toBe("{\"ok\":true}");
  });
});
