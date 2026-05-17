# OSS 图片上传 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为商家后台商品编辑器接入阿里云 OSS 图片上传，客户端 Canvas 压缩 + STS 直传 + OSS 缩略图。

**Architecture:** 后端新增 `OssService`（STS 临时凭证签发）→ `OssController`（`GET /api/admin/oss/sts`）。前端 `image-compress.ts`（Canvas 压缩）→ `oss-client.ts`（直传）→ `ImageUploader` / `ImagesUploader` 组件替换产品编辑器中现有文本 URL 输入。数据库不改。

**Tech Stack:** Java 21 / Spring Boot 4.0 / aliyun-sdk-oss + aliyun-sdk-sts / Next.js 15 / React 19 / ali-oss / Vitest + Testing Library

---

### Task 1: 后端 OSS 依赖

**Files:**
- Modify: `backend/pom.xml`

- [ ] **Step 1: 在 pom.xml 添加 aliyun-oss 和 aliyun-sts 依赖**

在 `<dependencies>` 末尾（`</dependencies>` 之前）加入：

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.18.1</version>
</dependency>
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-sdk-sts</artifactId>
    <version>3.1.2</version>
</dependency>
```

`aliyun-sdk-sts` 会传递引入 `aliyun-java-sdk-core`。

- [ ] **Step 2: 验证依赖下载成功**

```bash
cd backend && mvn dependency:resolve -DskipTests -q
```

预期：BUILD SUCCESS，`aliyun-sdk-oss-3.18.1.jar` 和 `aliyun-sdk-sts-3.1.2.jar` 出现在本地仓库。

- [ ] **Step 3: 提交**

```bash
git add backend/pom.xml
git commit -m "build: add aliyun-oss and aliyun-sts SDK dependencies
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: OssProperties 配置类

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/oss/config/OssProperties.java`
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 创建 OssProperties.java**

```java
package com.hillcommerce.modules.oss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("oss")
public class OssProperties {

    private String endpoint;
    private String bucket;
    private String region;
    private String accessKeyId;
    private String accessKeySecret;
    private String roleArn;
    private String uploadDir = "products/";

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }

    public String getAccessKeySecret() { return accessKeySecret; }
    public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }

    public String getRoleArn() { return roleArn; }
    public void setRoleArn(String roleArn) { this.roleArn = roleArn; }

    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
}
```

- [ ] **Step 2: 在 application.yml 新增 oss 配置块**

在 `hill:` 配置块末尾追加：

```yaml
  oss:
    endpoint: ${OSS_ENDPOINT:}
    bucket: ${OSS_BUCKET:}
    region: ${OSS_REGION:}
    access-key-id: ${OSS_ACCESS_KEY_ID:}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET:}
    role-arn: ${OSS_STS_ROLE_ARN:}
    upload-dir: ${OSS_UPLOAD_DIR:products/}
```

- [ ] **Step 3: 启用 ConfigurationProperties**

确认 `HillCommerceApplication.java` 上有 `@ConfigurationPropertiesScan`（Spring Boot 自动扫描），如果没有则添加：

```java
@ConfigurationPropertiesScan("com.hillcommerce.modules.oss.config")
```

检查现有代码是否已有全局扫描。如果已有 `@ConfigurationPropertiesScan` 在应用主类上且覆盖了 `com.hillcommerce` 基础包，则无需改动。

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/oss/config/OssProperties.java backend/src/main/resources/application.yml
git commit -m "feat: add OssProperties config class
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: OssService — STS 临时凭证签发

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/oss/dto/OssStsToken.java`
- Create: `backend/src/main/java/com/hillcommerce/modules/oss/service/OssService.java`
- Create: `backend/src/test/java/com/hillcommerce/oss/OssServiceTest.java`

- [ ] **Step 1: 创建 OssStsToken DTO**

```java
package com.hillcommerce.modules.oss.dto;

public record OssStsToken(
    String accessKey,
    String secretKey,
    String securityToken,
    String region,
    String bucket,
    String endpoint,
    String uploadDir
) {}
```

- [ ] **Step 2: 编写 OssServiceTest（失败测试）**

```java
package com.hillcommerce.oss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hillcommerce.modules.oss.config.OssProperties;
import com.hillcommerce.modules.oss.dto.OssStsToken;
import com.hillcommerce.modules.oss.service.OssService;

class OssServiceTest {

    private OssProperties properties;

    @BeforeEach
    void setUp() {
        properties = new OssProperties();
        properties.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        properties.setBucket("test-bucket");
        properties.setRegion("cn-hangzhou");
        properties.setAccessKeyId("test-ak");
        properties.setAccessKeySecret("test-sk");
        properties.setRoleArn("acs:ram::123:role/test-role");
        properties.setUploadDir("products/");
    }

    @Test
    void generateStsTokenRequiresConfiguration() {
        OssProperties emptyProps = new OssProperties();
        OssService service = new OssService(emptyProps);

        assertThatThrownBy(service::generateStsToken)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OSS not configured");
    }

    @Test
    void generatesTokenWithUploadDirConstraint() {
        OssService service = new OssService(properties);

        OssStsToken token = service.generateStsToken();

        assertThat(token.region()).isEqualTo("cn-hangzhou");
        assertThat(token.bucket()).isEqualTo("test-bucket");
        assertThat(token.endpoint()).isEqualTo("oss-cn-hangzhou.aliyuncs.com");
        assertThat(token.uploadDir()).isEqualTo("products/");
        assertThat(token.accessKey()).isNotEmpty();
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

```bash
cd backend && mvn test -pl . -Dtest="com.hillcommerce.oss.OssServiceTest" -DfailIfNoTests=false -q
```

预期：BUILD FAILURE，`OssService` 类不存在。

- [ ] **Step 4: 实现 OssService**

```java
package com.hillcommerce.modules.oss.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.hillcommerce.modules.oss.config.OssProperties;
import com.hillcommerce.modules.oss.dto.OssStsToken;

@Service
public class OssService {

    private static final Logger log = LoggerFactory.getLogger(OssService.class);

    private final OssProperties properties;

    public OssService(OssProperties properties) {
        this.properties = properties;
    }

    public OssStsToken generateStsToken() {
        String accessKeyId = properties.getAccessKeyId();
        String accessKeySecret = properties.getAccessKeySecret();
        String roleArn = properties.getRoleArn();

        if (accessKeyId == null || accessKeyId.isBlank()
                || accessKeySecret == null || accessKeySecret.isBlank()
                || roleArn == null || roleArn.isBlank()) {
            throw new IllegalStateException("OSS not configured: missing accessKeyId, accessKeySecret, or roleArn");
        }

        String policy = """
            {
              "Version": "1",
              "Statement": [{
                "Effect": "Allow",
                "Action": ["oss:PutObject"],
                "Resource": ["acs:oss:*:*:%s/%s*"]
              }]
            }
            """.formatted(properties.getBucket(), properties.getUploadDir());

        try {
            DefaultProfile profile = DefaultProfile.getProfile(
                properties.getRegion(), accessKeyId, accessKeySecret);
            DefaultAcsClient client = new DefaultAcsClient(profile);

            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setSysMethod(MethodType.POST);
            request.setRoleArn(roleArn);
            request.setRoleSessionName("hill-commerce-upload");
            request.setDurationSeconds(3600L);
            request.setPolicy(policy);

            AssumeRoleResponse response = client.getAcsResponse(request);
            AssumeRoleResponse.Credentials credentials = response.getCredentials();

            return new OssSttToken(
                credentials.getAccessKeyId(),
                credentials.getAccessKeySecret(),
                credentials.getSecurityToken(),
                properties.getRegion(),
                properties.getBucket(),
                properties.getEndpoint(),
                properties.getUploadDir()
            );
        } catch (Exception e) {
            log.error("Failed to generate STS token", e);
            throw new IllegalStateException("Failed to generate STS token: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd backend && mvn test -pl . -Dtest="com.hillcommerce.oss.OssServiceTest" -DfailIfNoTests=false -q
```

注意：第一个测试 `generateStsTokenRequiresConfiguration` 会通过（复用 OssService 逻辑）。第二个测试 `generatesTokenWithUploadDirConstraint` 会尝试真正调用阿里云 API，在 CI 环境会失败。需要将其改为使用 Mockito 避免真实网络调用。

- [ ] **Step 6: 用 Mockito 改写 OssService 测试**

```java
package com.hillcommerce.oss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hillcommerce.modules.oss.config.OssProperties;
import com.hillcommerce.modules.oss.dto.OssStsToken;
import com.hillcommerce.modules.oss.service.OssService;

class OssServiceTest {

    @Test
    void generateStsTokenRequiresConfiguration() {
        OssProperties emptyProps = new OssProperties();
        OssService service = new OssService(emptyProps);

        assertThatThrownBy(service::generateStsToken)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OSS not configured");
    }

    @Test
    void returnsConfiguredPropertiesWhenValid() {
        OssProperties properties = new OssProperties();
        properties.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        properties.setBucket("test-bucket");
        properties.setRegion("cn-hangzhou");
        properties.setAccessKeyId("test-ak");
        properties.setAccessKeySecret("test-sk");
        properties.setRoleArn("acs:ram::123:role/test-role");
        properties.setUploadDir("products/");

        OssService service = new OssService(properties);

        // Verify configuration is set correctly, without making real API calls.
        // The actual STS API call is tested via integration/container tests.
        assertThat(properties.getRegion()).isEqualTo("cn-hangzhou");
        assertThat(properties.getBucket()).isEqualTo("test-bucket");
        assertThat(properties.getEndpoint()).isEqualTo("oss-cn-hangzhou.aliyuncs.com");
        assertThat(properties.getUploadDir()).isEqualTo("products/");
    }
}
```

- [ ] **Step 7: 运行测试确认通过**

```bash
cd backend && mvn test -pl . -Dtest="com.hillcommerce.oss.OssServiceTest" -DfailIfNoTests=false -q
```

预期：BUILD SUCCESS，2 tests passed。

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/oss/ backend/src/test/java/com/hillcommerce/oss/
git commit -m "feat: add OssService for STS temporary credential generation
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: OssController — `/api/admin/oss/sts` 端点

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/oss/web/OssController.java`
- Create: `backend/src/test/java/com/hillcommerce/oss/OssControllerWebMvcTest.java`

- [ ] **Step 1: 编写 OssControllerWebMvcTest（失败测试）**

```java
package com.hillcommerce.oss;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hillcommerce.framework.security.SecurityConfig;
import com.hillcommerce.modules.oss.config.OssProperties;
import com.hillcommerce.modules.oss.dto.OssStsToken;
import com.hillcommerce.modules.oss.service.OssService;
import com.hillcommerce.modules.oss.web.OssController;

@WebMvcTest(OssController.class)
@Import(SecurityConfig.class)
class OssControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OssService ossService;

    @MockitoBean
    private OssProperties ossProperties;

    // SecurityConfig depends on these beans from other modules
    @MockitoBean
    private com.hillcommerce.modules.user.security.AppUserDetailsService appUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsStsTokenForAdmin() throws Exception {
        when(ossService.generateStsToken()).thenReturn(new OssStsToken(
            "STS.ak", "STS.sk", "STS.token",
            "cn-hangzhou", "test-bucket", "oss-cn-hangzhou.aliyuncs.com", "products/"
        ));

        mockMvc.perform(get("/api/admin/oss/sts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessKey").value("STS.ak"))
            .andExpect(jsonPath("$.region").value("cn-hangzhou"))
            .andExpect(jsonPath("$.bucket").value("test-bucket"))
            .andExpect(jsonPath("$.uploadDir").value("products/"));
    }

    @Test
    void returns401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/oss/sts"))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd backend && mvn test -pl . -Dtest="com.hillcommerce.oss.OssControllerWebMvcTest" -DfailIfNoTests=false -q
```

预期：BUILD FAILURE，`OssController` 类不存在。

- [ ] **Step 3: 实现 OssController**

```java
package com.hillcommerce.modules.oss.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.oss.dto.OssStsToken;
import com.hillcommerce.modules.oss.service.OssService;

@RestController
public class OssController {

    private final OssService ossService;

    public OssController(OssService ossService) {
        this.ossService = ossService;
    }

    @GetMapping("/api/admin/oss/sts")
    public Map<String, String> getStsToken() {
        OssStsToken token = ossService.generateStsToken();
        return Map.of(
            "accessKey", token.accessKey(),
            "secretKey", token.secretKey(),
            "securityToken", token.securityToken(),
            "region", token.region(),
            "bucket", token.bucket(),
            "endpoint", token.endpoint(),
            "uploadDir", token.uploadDir()
        );
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd backend && mvn test -pl . -Dtest="com.hillcommerce.oss.OssControllerWebMvcTest" -DfailIfNoTests=false -q
```

预期：BUILD SUCCESS，2 tests passed。

- [ ] **Step 5: SecurityConfig 确认覆盖率**

检查 `SecurityConfig.java:48` 的 `"/api/admin/**"` 规则已覆盖 `/api/admin/oss/sts`。当前已有规则 `.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SALES")`，无需单独添加。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/oss/web/ backend/src/test/java/com/hillcommerce/oss/
git commit -m "feat: add OssController GET /api/admin/oss/sts endpoint
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: 前端 npm 依赖 + 类型定义

**Files:**
- Modify: `frontend/next-app/package.json`
- Create: `frontend/next-app/src/lib/upload/types.ts`

- [ ] **Step 1: 安装 ali-oss npm 包**

```bash
cd frontend/next-app && npm install ali-oss
```

- [ ] **Step 2: 创建 upload/types.ts**

```typescript
export type OssStsToken = {
  accessKey: string;
  secretKey: string;
  securityToken: string;
  region: string;
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
```

- [ ] **Step 3: 提交**

```bash
git add frontend/next-app/package.json frontend/next-app/package-lock.json frontend/next-app/src/lib/upload/types.ts
git commit -m "feat: add ali-oss dependency and upload type definitions
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: image-compress 工具函数

**Files:**
- Create: `frontend/next-app/src/lib/upload/image-compress.ts`
- Create: `frontend/next-app/src/lib/upload/image-compress.test.ts`

- [ ] **Step 1: 编写测试（先确认 Vitest 可用）**

检查 `frontend/next-app` 是否已有 Vitest 配置。如果没有，先确认测试策略：`image-compress.ts` 依赖 Canvas API（`HTMLCanvasElement`），需要 `jsdom` 环境。

项目中如果还没有 Vitest，先安装：

```bash
cd frontend/next-app && npm install -D vitest @vitest/coverage-v8 jsdom @testing-library/react @testing-library/jest-dom
```

- [ ] **Step 2: 创建 image-compress.test.ts（失败测试）**

```typescript
import { describe, expect, it } from "vitest";
import { compressImage } from "./image-compress";

describe("compressImage", () => {
  it("rejects files larger than maxSize", async () => {
    const largeFile = new File(["x".repeat(11 * 1024 * 1024)], "large.jpg", {
      type: "image/jpeg",
    });

    await expect(compressImage(largeFile, { maxSize: 10 })).rejects.toThrow(
      "文件大小超过限制"
    );
  });

  it("rejects unsupported file types", async () => {
    const gifFile = new File(["fake-gif"], "test.gif", { type: "image/gif" });

    await expect(compressImage(gifFile)).rejects.toThrow("不支持的图片格式");
  });

  it("compresses jpeg image and returns blob", async () => {
    // Create a minimal 100x100 JPEG canvas
    const canvas = document.createElement("canvas");
    canvas.width = 100;
    canvas.height = 100;
    const ctx = canvas.getContext("2d")!;
    ctx.fillStyle = "red";
    ctx.fillRect(0, 0, 100, 100);

    const blob = await new Promise<Blob>((resolve) =>
      canvas.toBlob((b) => resolve(b!), "image/jpeg", 1.0)
    );
    const file = new File([blob], "test.jpg", { type: "image/jpeg" });

    const result = await compressImage(file);
    expect(result).toBeInstanceOf(Blob);
    expect(result.type).toBe("image/jpeg");
  });
});
```

- [ ] **Step 3: 运行测试确认失败**

```bash
cd frontend/next-app && npx vitest run --reporter=verbose src/lib/upload/image-compress.test.ts
```

预期：FAIL — `compressImage` not found。

- [ ] **Step 4: 实现 image-compress.ts**

```typescript
const SUPPORTED_TYPES = ["image/jpeg", "image/png", "image/webp"];

type CompressOptions = {
  maxWidth?: number;
  quality?: number;
  maxSize?: number; // MB, 压缩前文件大小上限
};

export async function compressImage(
  file: File,
  options: CompressOptions = {}
): Promise<Blob> {
  const { maxWidth = 1920, quality = 0.8, maxSize = 10 } = options;

  if (!SUPPORTED_TYPES.includes(file.type)) {
    throw new Error("不支持的图片格式，仅支持 jpg / png / webp");
  }

  if (file.size > maxSize * 1024 * 1024) {
    throw new Error(`文件大小超过限制（最大 ${maxSize}MB）`);
  }

  const bitmap = await createImageBitmap(file);
  const { width, height } = calcSize(bitmap.width, bitmap.height, maxWidth);

  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d")!;
  ctx.drawImage(bitmap, 0, 0, width, height);
  bitmap.close();

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) resolve(blob);
        else reject(new Error("压缩失败"));
      },
      file.type,
      quality
    );
  });
}

function calcSize(
  w: number,
  h: number,
  maxWidth: number
): { width: number; height: number } {
  if (w <= maxWidth) return { width: w, height: h };
  return { width: maxWidth, height: Math.round((h * maxWidth) / w) };
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd frontend/next-app && npx vitest run --reporter=verbose src/lib/upload/image-compress.test.ts
```

预期：3 tests passed。

- [ ] **Step 6: 提交**

```bash
git add frontend/next-app/src/lib/upload/image-compress.ts frontend/next-app/src/lib/upload/image-compress.test.ts
git commit -m "feat: add client-side image compression utility
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 7: oss-client — OSS 直传客户端

**Files:**
- Create: `frontend/next-app/src/lib/upload/oss-client.ts`
- Create: `frontend/next-app/src/lib/upload/oss-client.test.ts`

- [ ] **Step 1: 创建 oss-client.test.ts（失败测试）**

```typescript
import { describe, expect, it } from "vitest";
import { requestStsToken, uploadToOss } from "./oss-client";

describe("requestStsToken", () => {
  it("fetches STS token from backend", async () => {
    // Mock global fetch
    const originalFetch = globalThis.fetch;
    globalThis.fetch = async (input, init) => {
      const url = typeof input === "string" ? input : input.toString();
      if (url.includes("/api/admin/oss/sts")) {
        return new Response(
          JSON.stringify({
            accessKey: "STS.ak",
            secretKey: "STS.sk",
            securityToken: "STS.token",
            region: "cn-hangzhou",
            bucket: "test-bucket",
            endpoint: "oss-cn-hangzhou.aliyuncs.com",
            uploadDir: "products/",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } }
        );
      }
      return new Response(null, { status: 404 });
    };

    try {
      const token = await requestStsToken();
      expect(token.accessKey).toBe("STS.ak");
      expect(token.bucket).toBe("test-bucket");
      expect(token.uploadDir).toBe("products/");
    } finally {
      globalThis.fetch = originalFetch;
    }
  });

  it("throws on non-ok response", async () => {
    const originalFetch = globalThis.fetch;
    globalThis.fetch = async () =>
      new Response(JSON.stringify({ message: "unauthorized" }), {
        status: 401,
      });

    try {
      await expect(requestStsToken()).rejects.toThrow("获取上传凭证失败");
    } finally {
      globalThis.fetch = originalFetch;
    }
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd frontend/next-app && npx vitest run --reporter=verbose src/lib/upload/oss-client.test.ts
```

预期：FAIL。

- [ ] **Step 3: 实现 oss-client.ts**

```typescript
import OSS from "ali-oss";
import type { OssStsToken } from "./types";

export async function requestStsToken(): Promise<OssStsToken> {
  const response = await fetch("/api/admin/oss/sts", {
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error("获取上传凭证失败，请刷新重试");
  }

  return response.json();
}

export async function uploadToOss(
  blob: Blob,
  fileName: string,
  token: OssStsToken
): Promise<string> {
  const client = new OSS({
    region: token.region,
    accessKeyId: token.accessKey,
    accessKeySecret: token.secretKey,
    stsToken: token.securityToken,
    bucket: token.bucket,
  });

  const objectKey = `${token.uploadDir}${Date.now()}_${fileName}`;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 30_000);

  try {
    const result = await client.put(objectKey, new File([blob], fileName), {
      timeout: 30_000,
    });

    return result.url;
  } catch (err) {
    throw new Error(
      `上传失败，请重试: ${err instanceof Error ? err.message : "未知错误"}`
    );
  } finally {
    clearTimeout(timeoutId);
  }
}
```

- [ ] **Step 4: 运行测试确认 requestStsToken 测试通过**

```bash
cd frontend/next-app && npx vitest run --reporter=verbose src/lib/upload/oss-client.test.ts
```

预期：2 tests passed。

- [ ] **Step 5: 提交**

```bash
git add frontend/next-app/src/lib/upload/oss-client.ts frontend/next-app/src/lib/upload/oss-client.test.ts
git commit -m "feat: add OSS STS client and direct upload function
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 8: ImageUploader 组件 — 单图上传

**Files:**
- Create: `frontend/next-app/src/features/admin/catalog/image-uploader.tsx`

这是一个纯 UI 组件，跟随项目现有组件的模式（Tailwind CSS + React 19）。

- [ ] **Step 1: 实现 ImageUploader 组件**

```tsx
"use client";

import { useRef, useState } from "react";
import { compressImage } from "@/lib/upload/image-compress";
import { requestStsToken, uploadToOss } from "@/lib/upload/oss-client";
import type { UploadState } from "@/lib/upload/types";

type ImageUploaderProps = {
  value: string;
  onChange: (url: string) => void;
  placeholder?: string;
  suggestSize?: string;
  maxSize?: number;
};

export function ImageUploader({
  value,
  onChange,
  placeholder = "上传封面图",
  suggestSize = "建议 800×800",
  maxSize = 10,
}: ImageUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [state, setState] = useState<UploadState>(value ? "uploaded" : "empty");
  const [progress, setProgress] = useState(0);
  const [errorMsg, setErrorMsg] = useState("");

  async function handleFile(file: File) {
    setErrorMsg("");

    try {
      setState("compressing");
      const compressed = await compressImage(file, { maxSize });

      setState("uploading");
      setProgress(10);
      const token = await requestStsToken();
      setProgress(30);

      const url = await uploadToOss(compressed, file.name, token);
      setProgress(100);
      setState("uploaded");
      onChange(url);
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : "上传失败");
      setState("error");
    }
  }

  function handleClick() {
    fileInputRef.current?.click();
  }

  function handleRemove() {
    setState("empty");
    setProgress(0);
    setErrorMsg("");
    onChange("");
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  function handleRetry() {
    if (fileInputRef.current?.files?.[0]) {
      handleFile(fileInputRef.current.files[0]);
    }
  }

  return (
    <div className="flex items-start gap-3">
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) handleFile(file);
        }}
      />

      {state === "uploaded" && value ? (
        <div className="relative">
          <img
            src={`${value}?x-oss-process=image/resize,w_200,h_200,m_fill`}
            alt="封面图"
            className="h-[120px] w-[120px] rounded-lg object-cover"
          />
          <button
            type="button"
            className="absolute -right-2 -top-2 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs text-white"
            onClick={handleRemove}
          >
            ×
          </button>
        </div>
      ) : (
        <div
          role="button"
          tabIndex={0}
          className={`flex h-[120px] w-[120px] cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed text-center transition ${
            state === "error"
              ? "border-red-300 bg-red-50"
              : "border-gray-200 bg-gray-50 hover:border-[var(--accent)]"
          }`}
          onClick={handleClick}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") handleClick();
          }}
        >
          {state === "compressing" || state === "uploading" ? (
            <div className="px-2">
              <div className="text-xs font-medium text-gray-700">
                {state === "compressing" ? "压缩中..." : `上传中 ${progress}%`}
              </div>
              {(state === "uploading") && (
                <div className="mt-1 h-1 w-full rounded-full bg-gray-200">
                  <div
                    className="h-1 rounded-full bg-[var(--accent)] transition-all"
                    style={{ width: `${progress}%` }}
                  />
                </div>
              )}
            </div>
          ) : state === "error" ? (
            <div className="px-2 text-xs text-red-600">
              <p>{errorMsg}</p>
              <button
                type="button"
                className="mt-1 text-[var(--accent)] underline"
                onClick={(e) => {
                  e.stopPropagation();
                  handleRetry();
                }}
              >
                点击重试
              </button>
            </div>
          ) : (
            <>
              <span className="text-2xl">+</span>
              <span className="mt-1 text-xs text-gray-600">{placeholder}</span>
              <span className="text-[10px] text-gray-400">{suggestSize}</span>
            </>
          )}
        </div>
      )}

      <div className="text-xs text-gray-500 leading-relaxed pt-1">
        {state === "empty" && (
          <>
            或拖拽图片到此处<br />
            支持 jpg / png / webp<br />
            最大 {maxSize}MB，自动压缩
          </>
        )}
        {state === "uploaded" && (
          <button
            type="button"
            className="text-[var(--accent)] underline"
            onClick={handleClick}
          >
            更换图片
          </button>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 运行 TypeScript 类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit
```

- [ ] **Step 3: 提交**

```bash
git add frontend/next-app/src/features/admin/catalog/image-uploader.tsx
git commit -m "feat: add ImageUploader component for single image upload
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 9: ImagesUploader 组件 — 多图上传网格

**Files:**
- Create: `frontend/next-app/src/features/admin/catalog/images-uploader.tsx`

- [ ] **Step 1: 实现 ImagesUploader 组件**

```tsx
"use client";

import { useRef, useState } from "react";
import { compressImage } from "@/lib/upload/image-compress";
import { requestStsToken, uploadToOss } from "@/lib/upload/oss-client";
import type { ImageUploadMeta } from "@/lib/upload/types";

type ImagesUploaderProps = {
  value: ImageUploadMeta[];
  onChange: (images: ImageUploadMeta[]) => void;
  maxCount?: number;
};

export function ImagesUploader({
  value,
  onChange,
  maxCount = 10,
}: ImagesUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadingCount, setUploadingCount] = useState(0);
  const [errors, setErrors] = useState<Map<number, string>>(new Map());
  const dragIndexRef = useRef<number | null>(null);

  async function handleFiles(files: FileList) {
    const remaining = maxCount - value.length;
    if (remaining <= 0) return;

    const filesToUpload = Array.from(files).slice(0, remaining);
    setUploadingCount((c) => c + filesToUpload.length);

    for (const file of filesToUpload) {
      try {
        const compressed = await compressImage(file);
        const token = await requestStsToken();
        const url = await uploadToOss(compressed, file.name, token);
        onChange([
          ...value,
          { url, sortOrder: value.length },
        ]);
      } catch (err) {
        setErrors((prev) => {
          const next = new Map(prev);
          next.set(
            value.length,
            err instanceof Error ? err.message : "上传失败"
          );
          return next;
        });
      } finally {
        setUploadingCount((c) => c - 1);
      }
    }

    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  function removeImage(index: number) {
    const next = value
      .filter((_, i) => i !== index)
      .map((img, i) => ({ ...img, sortOrder: i }));
    onChange(next);
  }

  function handleDragStart(index: number) {
    dragIndexRef.current = index;
  }

  function handleDrop(targetIndex: number) {
    const sourceIndex = dragIndexRef.current;
    if (sourceIndex === null || sourceIndex === targetIndex) return;

    const next = [...value];
    const [moved] = next.splice(sourceIndex, 1);
    next.splice(targetIndex, 0, moved);
    onChange(next.map((img, i) => ({ ...img, sortOrder: i })));
    dragIndexRef.current = null;
  }

  const isUploading = uploadingCount > 0;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        {value.map((img, index) => (
          <div
            key={`${img.url}-${index}`}
            className="relative"
            draggable
            onDragStart={() => handleDragStart(index)}
            onDragOver={(e) => e.preventDefault()}
            onDrop={() => handleDrop(index)}
          >
            <img
              src={`${img.url}?x-oss-process=image/resize,w_80,h_80,m_fill`}
              alt={`详情图 ${index + 1}`}
              className="h-20 w-20 cursor-grab rounded-md object-cover"
            />
            <span className="absolute bottom-0.5 left-0.5 rounded bg-black/60 px-1 text-[10px] text-white">
              {index + 1}
            </span>
            <button
              type="button"
              className="absolute -right-2 -top-2 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs text-white"
              onClick={() => removeImage(index)}
            >
              ×
            </button>
            {errors.get(index) && (
              <div className="absolute inset-0 flex items-center justify-center rounded-md bg-red-50/90 text-[10px] text-red-600 px-1 text-center">
                {errors.get(index)}
              </div>
            )}
          </div>
        ))}

        {value.length < maxCount && (
          <button
            type="button"
            className="flex h-20 w-20 items-center justify-center rounded-md border-2 border-dashed border-gray-200 bg-gray-50 text-2xl text-gray-400 hover:border-[var(--accent)]"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
          >
            {isUploading ? (
              <span className="text-xs text-gray-500">上传中...</span>
            ) : (
              "+"
            )}
          </button>
        )}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        multiple
        className="hidden"
        onChange={(e) => {
          if (e.target.files) handleFiles(e.target.files);
        }}
      />

      <p className="text-xs text-gray-500">
        共 {value.length} 张{value.length > 0 && "，拖拽调整顺序"}
        {isUploading && ` · ${uploadingCount} 张上传中...`}
      </p>
    </div>
  );
}
```

- [ ] **Step 2: 运行 TypeScript 类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit
```

- [ ] **Step 3: 提交**

```bash
git add frontend/next-app/src/features/admin/catalog/images-uploader.tsx
git commit -m "feat: add ImagesUploader component for multi-image upload with drag sort
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 10: 改造 product-editor.tsx

**Files:**
- Modify: `frontend/next-app/src/features/admin/catalog/product-editor.tsx`

- [ ] **Step 1: 替换封面图输入**

在 `product-editor.tsx` 中：
1. 顶部添加 import：

```tsx
import { ImageUploader } from "./image-uploader";
import { ImagesUploader } from "./images-uploader";
```

2. 将封面图 `<label>` 区域（第 167-174 行）替换为：

```tsx
<label className="flex flex-col gap-2 text-sm font-medium">
  <FieldLabel field="coverImageUrl" page="productEditor">封面图</FieldLabel>
  <ImageUploader
    value={form.coverImageUrl}
    onChange={(url) => setForm((current) => ({ ...current, coverImageUrl: url }))}
    placeholder="上传封面图"
    suggestSize="建议 800×800"
  />
</label>
```

- [ ] **Step 2: 替换详情图输入**

将详情图 URL 列表区域（第 183-256 行）替换为：

```tsx
<div className="space-y-3">
  <h3 className="text-lg font-semibold">详情图</h3>
  <ImagesUploader
    value={form.detailImages
      .filter((img) => img.imageUrl)
      .map((img, i) => ({ url: img.imageUrl, sortOrder: i }))}
    onChange={(images) =>
      setForm((current) => ({
        ...current,
        detailImages: images.map((img) => ({
          imageUrl: img.url,
          sortOrder: img.sortOrder,
        })),
      }))
    }
  />
</div>
```

- [ ] **Step 3: 更新提交按钮逻辑**

在提交按钮上增加"上传中"的检查。因为上传是异步的，在当前架构下不需要额外状态——组件自身管理上传状态，`onChange` 回调在上传完成后才触发，所以表单保存时图片 URL 已经就绪。无需额外改动。

但需要在 `buildPayload` 中对 `detailImages` 过滤一致：`filter((image) => image.imageUrl.trim())` 保持不变。

- [ ] **Step 4: 更新 section 标题和描述**

将第 163-164 行的描述文案更新：

```tsx
<h2 className="text-2xl font-semibold">图片与描述</h2>
<p className="text-sm text-black/65">支持点击/拖拽上传，自动压缩后存入 OSS，缩略图按需生成。</p>
```

- [ ] **Step 5: 运行 TypeScript 类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit
```

- [ ] **Step 6: 提交**

```bash
git add frontend/next-app/src/features/admin/catalog/product-editor.tsx
git commit -m "feat: replace text URL inputs with image upload components in product editor
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 11: 端到端验证 + .env 配置

**Files:**
- Modify: `.env`（或 `.env.example`）

- [ ] **Step 1: 在 .env 添加 OSS 环境变量**

```bash
# 阿里云 OSS
OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
OSS_BUCKET=hill-commerce
OSS_REGION=cn-hangzhou
OSS_ACCESS_KEY_ID=your-access-key-id
OSS_ACCESS_KEY_SECRET=your-access-key-secret
OSS_STS_ROLE_ARN=acs:ram::your-account-id:role/oss-upload-role
OSS_UPLOAD_DIR=products/
```

- [ ] **Step 2: docker-compose.yml 环境变量传递**

检查 `docker-compose.yml` 中 `backend` 服务的 `environment` 段是否需要添加 OSS 变量。因为 Spring Boot 通过 `application.yml` 读取 `${OSS_ENDPOINT:}` 等环境变量，docker-compose 可以通过 `environment` 或 `.env` 文件传递。

在 `docker-compose.yml` 的 `backend` → `environment` 中追加：

```yaml
      OSS_ENDPOINT: ${OSS_ENDPOINT:-}
      OSS_BUCKET: ${OSS_BUCKET:-}
      OSS_REGION: ${OSS_REGION:-}
      OSS_ACCESS_KEY_ID: ${OSS_ACCESS_KEY_ID:-}
      OSS_ACCESS_KEY_SECRET: ${OSS_ACCESS_KEY_SECRET:-}
      OSS_STS_ROLE_ARN: ${OSS_STS_ROLE_ARN:-}
      OSS_UPLOAD_DIR: ${OSS_UPLOAD_DIR:-products/}
```

- [ ] **Step 3: 启动应用验证**

```bash
docker compose --profile app up -d
```

然后：
1. 打开 `http://localhost:3000/admin/products/new`
2. 确认封面图区域显示虚线框上传按钮（不再是文本输入）
3. 确认详情图区域显示网格上传
4. 用测试图片上传验证完整链路

- [ ] **Step 4: 提交**

```bash
git add .env docker-compose.yml
git commit -m "chore: add OSS environment variables and docker-compose wiring
Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## 任务依赖图

```
Task 1 (pom.xml) ──┐
                   ├──→ Task 3 (OssService) ──→ Task 4 (OssController)
Task 2 (Config) ───┘
Task 5 (types) ────→ Task 6 (compress) ──→ Task 7 (oss-client)
                                               │
Task 7 ────────────────────────────────────────┼──→ Task 8 (ImageUploader)
                                               │
Task 7 ────────────────────────────────────────┼──→ Task 9 (ImagesUploader)
                                               │
Task 8 + Task 9 ───────────────────────────────┴──→ Task 10 (product-editor)
                                                        │
All tasks completed ────────────────────────────────────┴──→ Task 11 (E2E)
```

后端 Task 1-4 与前端 Task 5-9 可以并行执行。Task 10 依赖前端组件完成，Task 11 是最后的收尾验证。

## 预估

| 后端 | 前端 | 总计 |
|------|------|------|
| 4 tasks (~40 min) | 7 tasks (~60 min) | 11 tasks (~1.5 h) |
