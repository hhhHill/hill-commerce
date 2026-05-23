# CI/CD 排障手册

按错误信息快速定位问题。

---

## GitHub Actions 阶段

### `Error: Unable to resolve action ... unable to find version`

**信息示例**：`Unable to resolve action 'appleboy/scp-action@v0', unable to find version 'v0'`

**根因**：action 的版本标签不存在。

**修复**：使用 `@master` 或查 GitHub Marketplace 确认最新版本号。

---

### `ERROR: failed to build: invalid tag ... repository name must be lowercase`

**信息示例**：`invalid tag "ghcr.io/hhhHill/hill-commerce-backend:latest": repository name must be lowercase`

**根因**：Docker 镜像标签必须全小写，GitHub 用户名有大写字母。

**修复**：用 `bash ${OWNER,,}` 把仓库 owner 转小写。否则改用不区分大小写的 ACR。

---

### `installation not allowed to Create organization package`

**根因**：`GITHUB_TOKEN` 默认没有 `packages:write` 权限。

**修复**：在 deploy job 中添加：
```yaml
permissions:
  contents: read
  packages: write
```

---

### `unauthorized: authentication required`（ACR）

**根因**：ACR 用户名或密码不对。

**排查**：
1. ACR 控制台 → 访问凭证 → 看清楚**用户名**（不是阿里云账号密码）
2. 重置固定密码，复制到 `ACR_PASSWORD`
3. 本地先 `docker login` 验证

---

## SSH / SCP 阶段

### `ssh: no key found` / `ssh: unable to authenticate`

**信息示例**：
```
ssh.ParsePrivateKey: ssh: no key found
ssh: handshake failed: ssh: unable to authenticate
```

**根因**：`VPS_SSH_KEY` secret 中私钥格式有问题——换行符丢失或被转换成 CRLF。

**修复**：
1. 不要用记事本粘贴密钥
2. 用 VS Code 打开私钥文件，全选复制，在 GitHub Secrets 网页里直接粘贴
3. 或者把 ED25519 密钥转成 PEM 格式兼容性更好：
```bash
ssh-keygen -p -m PEM -f ~/.ssh/github-actions
cat ~/.ssh/github-actions
```

---

### `Run Command Timeout`（拉镜像超时）

**信息示例**：拉取 Docker 镜像超时

**根因**：国内 VPS 从 ghcr.io 拉镜像太慢。

**修复**：改用**阿里云 ACR**（同地域秒拉）。

---

## VPS 部署阶段

### Application error / `ECONNREFUSED`

**信息示例**：
```
TypeError: fetch failed
[cause]: [AggregateError] { code: 'ECONNREFUSED' }
```

**根因**：Next.js 服务端（SSR）通过默认 `localhost:8080` 请求后端，但在 Docker 容器里 `localhost` 指向自己。

**修复**：在 docker-compose.yml 前端服务中加环境变量：
```yaml
environment:
  HILL_BACKEND_URL: http://backend:8080
```

---

### `docker compose up` 没有启前端和后端

**根因**：后端、前端、nginx 都在 `profiles: ["app"]` 下，需要加 `--profile app`。

**修复**：
```bash
docker compose --profile app up -d --remove-orphans
```

---

### `Error response from daemon: ... pull access denied`

**根因**：VPS 上没登录 ACR。

**修复**：
```bash
docker login <ACR_REGISTRY> -u <ACR_USERNAME>
```

---

## 后端测试阶段

### 403 `Status expected:<201> but was:<403>`

**信息示例**：测试调用 `/api/admin/categories` 返回 403。

**根因**：控制器方法上有 `@RequireRole("ADMIN")` 注解，但测试以 MERCHANT 身份访问。SecurityConfig 的 `hasAnyRole("ADMIN", "MERCHANT")` 被方法级 AOP 覆盖。

**排查**：
1. 检查对应 Controller 的 `@RequireRole` 注解
2. 检查 `RoleAspect.java` 的切面逻辑

**修复**：将 `@RequireRole("ADMIN")` 改为 `@RequireRole({"ADMIN", "MERCHANT"})`。

---

### Testcontainers `ContainerLaunch Container startup failed`

**信息示例**：`Container startup failed for image mysql:9.4.0`

**根因**：CI 的 `services:` 已提供 MySQL，Testcontainers 在 GitHub Actions 里又额外启动容器，Docker-in-Docker 环境不兼容。

**修复**：移除 `@Testcontainers`、`@Container`、`MySQLContainer` 字段，`@DynamicPropertySource` 改为指向 CI 的 MySQL 服务（`localhost:3306`）。

---

### `DataIntegrityViolation` / `Cannot delete or update a parent row`

**信息示例**：`Cannot delete or update a parent row: a foreign key constraint fails (payments, CONSTRAINT fk_payments_order)`

**根因**：清理 SQL 中 `delete from orders` 在 `delete from payments` 之前执行，外键约束阻止删除。

**修复**：在 @BeforeEach 的清理 SQL 中，`delete from payments` 必须在 `delete from orders` 之前。

---

## 前端测试阶段

### `No "useSearchParams" export is defined on the "next/navigation" mock`

**根因**：vitest mock 不完整，`AdminSidebar` 组件调用了 `useSearchParams()` 但 mock 里没导出。

**修复**：
```typescript
vi.mock("next/navigation", () => ({
  usePathname: () => "/admin/products",
  useSearchParams: () => new URLSearchParams(""),
  useRouter: () => ({ refresh: vi.fn() })
}));
```

---

### `No test suite found in file`

**根因**：测试文件使用了 Node.js 原生 `import test from "node:test"` 和 `import assert from "node:assert/strict"`，vitest 不识别。

**修复**：将 `node:test` / `node:assert` 改为 vitest：
```typescript
// 之前
import test from "node:test";
import assert from "node:assert/strict";
test("xxx", () => { assert.equal(a, b); });

// 之后
import { describe, it, expect } from "vitest";
it("xxx", () => { expect(a).toBe(b); });
```

TSX 文件还需加上 `// @vitest-environment jsdom`。
