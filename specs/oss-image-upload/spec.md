# OSS 图片上传服务

## 概述

为商家后台商品编辑器接入阿里云 OSS，替换当前纯文本 URL 输入，实现真正的图片上传功能。
采用**客户端直传 + STS 临时凭证**架构，上传前浏览器端 Canvas 压缩，缩略图通过 OSS 图片处理按需生成。

## 架构

```
浏览器（商家后台）
  │
  ├─ 1. GET /api/admin/oss/sts  ──→  后端 OssController
  │    返回 {accessKey, secretKey, securityToken, region, bucket, endpoint, uploadDir}
  │
  ├─ 2. Canvas 压缩（最大 1920px, 质量 0.8）→ OSS JS SDK 直传
  │
  └─ 3. 保存商品时，OSS key → URL 存入数据库
        缩略图通过 URL 参数按需生成：
        ?x-oss-process=image/resize,w_200,h_200
```

## 后端

### 依赖

`backend/pom.xml` 新增：
- `aliyun-sdk-oss` — OSS Java SDK
- `aliyun-sdk-sts` — STS 临时凭证 SDK

### 配置

`application.yml` 新增：

```yaml
oss:
  endpoint: ${OSS_ENDPOINT}
  bucket: ${OSS_BUCKET}
  region: ${OSS_REGION}
  access-key-id: ${OSS_ACCESS_KEY_ID}
  access-key-secret: ${OSS_ACCESS_KEY_SECRET}
  role-arn: ${OSS_STS_ROLE_ARN}
  upload-dir: products/
```

`.env` 新增对应的环境变量。

### 新建文件

| 文件 | 职责 |
|------|------|
| `config/OssProperties.java` | `@ConfigurationProperties("oss")` 配置类 |
| `service/OssService.java` | 调用 STS SDK 签发临时凭证，返回 `OssStsToken` |
| `controller/OssController.java` | `GET /api/admin/oss/sts`，返回 STS 凭证 JSON |

### API

```
GET /api/admin/oss/sts
→ 200 {
    accessKey: "STS.xxx",
    secretKey: "xxx",
    securityToken: "xxx",
    region: "oss-cn-hangzhou",
    bucket: "hill-commerce",
    endpoint: "oss-cn-hangzhou.aliyuncs.com",
    uploadDir: "products/"
  }
```

- 仅 ADMIN/SALES 角色可访问
- STS 凭证有效期 1 小时
- 上传目录限制在 `products/*`

### 安全

- STS Policy 限制只能 PutObject 到指定 bucket 的 `products/` 前缀下
- 临时凭证有效期 1 小时，过期后前端自动续期
- 后端不处理文件流，不暴露主账号 AK

## 前端

### 新建文件

| 文件 | 职责 |
|------|------|
| `lib/upload/types.ts` | 类型定义：UploadState, OssStsToken, UploadResult |
| `lib/upload/image-compress.ts` | Canvas API 图片压缩工具函数 |
| `lib/upload/oss-client.ts` | STS 获取 + OSS JS SDK 直传封装 |
| `features/admin/catalog/image-uploader.tsx` | 单图上传组件（封面图使用） |
| `features/admin/catalog/images-uploader.tsx` | 多图上传组件（详情图使用，网格排列+拖拽排序） |

### 工具库

#### image-compress.ts

```
compressImage(file: File, options?: {
  maxWidth?: number   // 默认 1920
  quality?: number    // 默认 0.8
  maxSize?: number    // 默认 10（MB，压缩前文件大小上限）
}): Promise<Blob>
```

内部使用 Canvas `drawImage` + `toBlob` 实现。

#### oss-client.ts

```
requestStsToken(): Promise<OssStsToken>
uploadToOss(file: Blob, fileName: string, token: OssStsToken): Promise<string>
```

- `requestStsToken`：调 `/api/admin/oss/sts`
- `uploadToOss`：用 aliyun-oss SDK 直传，object key 加随机后缀防冲突，带 30 秒超时

### 组件

#### ImageUploader

```
Props:
  value: string        // 当前 OSS URL（空表示未上传）
  onChange: (url: string) => void
  placeholder?: string // 默认 "上传封面图"
  suggestSize?: string // 默认 "建议 800×800"
  maxSize?: number     // 默认 10MB（压缩前）
```

状态：`empty | compressing | uploading | uploaded | error`

- empty：虚线框 + 点击/拖拽
- compressing：显示 "压缩中..."
- uploading：进度条 + 百分比
- uploaded：缩略图预览 + "更换/删除"
- error：错误提示 + 点击重试

#### ImagesUploader

```
Props:
  value: {url: string, sortOrder: number}[]
  onChange: (images: {url: string, sortOrder: number}[]) => void
  maxCount?: number  // 默认 10
```

状态：`empty | uploading(N/M) | error(第i张)`

- 网格布局，每格 80×80 缩略图
- 支持批量选择文件
- 已上传图片左上角显示序号，右上角 × 删除
- HTML5 drag-and-drop 排序（每次拖拽后更新 sortOrder）
- 末尾始终有 + 按钮添加图片
- 提交前有图片在上传中时按钮置灰 + 提示

### 改造文件

**product-editor.tsx**：
- 封面图 `<input>` → `<ImageUploader value={coverImageUrl} onChange={setCoverImageUrl} />`
- 详情图文本列表 → `<ImagesUploader value={detailImages} onChange={setDetailImages} />`

## 图片处理

### 客户端压缩

- 上传前自动压缩，限制最大宽度 1920px，质量 0.8
- 仅处理 `image/jpeg`、`image/png`、`image/webp` 格式
- 文件超过 10MB（压缩前）直接拒绝，提示用户

### OSS 缩略图

- 列表/网格等缩略图场景使用 URL 参数按需生成：
  - 小缩略图：`?x-oss-process=image/resize,w_200,h_200,m_fill`
  - 中等缩略图：`?x-oss-process=image/resize,w_400`
  - 详情原图：直接使用原始 URL
- 不预生成缩略图，节省存储

## 错误处理

| 异常 | 处理 |
|------|------|
| STS 凭证获取失败 | 提示"获取上传凭证失败，请刷新重试"，不阻塞编辑器 |
| 文件类型不符 | 选择时前端校验拦截，仅允许 jpg/png/webp |
| 文件过大 | 超 10MB 直接拦截，提示限制 |
| OSS 上传失败 | 提示"上传失败，请重试"，保留文件引用支持重传 |
| 上传超时 | 30 秒超时，显示失败状态 |
| 未上传完成就提交 | 按钮置灰 + "还有 N 张图片正在上传中" |

## 数据库

**不变。** 现有列直接存储 OSS URL：
- `products.cover_image_url VARCHAR(512)`
- `product_images.image_url VARCHAR(512)`

## 测试策略

| 层级 | 内容 | 工具 |
|------|------|------|
| `image-compress.ts` | 压缩后尺寸/质量/格式验证 | Vitest + jsdom Canvas |
| `oss-client.ts` | STS 请求 + 上传流程 | Vitest + mock fetch |
| `OssService.java` | STS 签发正确性 | JUnit 5 + Mockito |
| `OssController.java` | API 返回结构、权限 | MockMvc `@WebMvcTest` |
| `ImageUploader` | 各状态渲染、用户交互 | Vitest + Testing Library |
| `ImagesUploader` | 多图上传、排序、删除 | Vitest + Testing Library |

## 不做的

- 水印功能
- 视频/其他文件类型上传
- 用户头像/店铺 banner 等其他图片场景
- 预生成缩略图存储
