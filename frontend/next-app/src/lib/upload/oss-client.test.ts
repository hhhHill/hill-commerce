import { beforeEach, describe, expect, it, vi } from "vitest";

import { clearStsCache, requestStsToken, uploadToOss } from "./oss-client";
import type { OssStsToken } from "./types";

const putMock = vi.fn();

vi.mock("ali-oss", () => ({
  default: vi.fn(function OSSMock() {
    return {
      put: putMock
    };
  })
}));

const stsPayload = {
  accessKey: "STS.ak",
  secretKey: "STS.sk",
  securityToken: "STS.token",
  ossRegion: "oss-cn-hangzhou",
  bucket: "test-bucket",
  endpoint: "oss-cn-hangzhou.aliyuncs.com",
  uploadDir: "products/"
};

describe("requestStsToken", () => {
  beforeEach(() => {
    clearStsCache();
  });

  it("fetches STS token from backend", async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(stsPayload), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );

    const token = await requestStsToken();

    expect(token.accessKey).toBe("STS.ak");
    expect(global.fetch).toHaveBeenCalledWith("/api/admin/oss/sts", {
      credentials: "include"
    });
  });

  it("throws a user facing error when token request fails", async () => {
    global.fetch = vi.fn().mockResolvedValue(new Response(null, { status: 401 }));

    await expect(requestStsToken()).rejects.toThrow("获取上传凭证失败");
  });

  it("reuses cached token within expiry window", async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(stsPayload), { status: 200 })
    );

    await requestStsToken();
    await requestStsToken();

    expect(global.fetch).toHaveBeenCalledTimes(1);
  });
});

describe("uploadToOss", () => {
  it("uploads with a generated object key and returns the OSS url", async () => {
    putMock.mockResolvedValue({ name: "products/test.jpg", url: "https://img.example.com/test.jpg" });
    const token: OssStsToken = {
      accessKey: "STS.ak",
      secretKey: "STS.sk",
      securityToken: "STS.token",
      ossRegion: "oss-cn-hangzhou",
      bucket: "test-bucket",
      endpoint: "oss-cn-hangzhou.aliyuncs.com",
      uploadDir: "products/"
    };

    const url = await uploadToOss(new Blob(["image"], { type: "image/jpeg" }), "test.jpg", token);

    expect(url).toBe("https://img.example.com/test.jpg");
    expect(putMock.mock.calls[0][0]).toMatch(/^products\/\d+_[a-z0-9]+_test\.jpg$/);
    expect(putMock.mock.calls[0][2]).toEqual({ timeout: 30_000 });
  });
});
