import test from "node:test";
import assert from "node:assert/strict";

import { getProxyResponseBody } from "./proxy-response";

test("omits response body for 204 responses", () => {
  const body = getProxyResponseBody(204, "");
  const response = new Response(body, { status: 204 });

  assert.equal(body, null);
  assert.equal(response.status, 204);
});

test("preserves response body for non-empty non-204 responses", async () => {
  const body = getProxyResponseBody(200, "{\"ok\":true}");
  const response = new Response(body, {
    status: 200,
    headers: {
      "content-type": "application/json"
    }
  });

  assert.equal(body, "{\"ok\":true}");
  assert.equal(await response.text(), "{\"ok\":true}");
});
