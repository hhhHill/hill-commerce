# Tasks: oss-image-upload

**Status**: active

## Goal

为商家后台商品编辑器接入阿里云 OSS 图片上传，客户端 Canvas 压缩 + STS 直传 + OSS 缩略图。替换当前纯文本 URL 输入。

## Implementation Order

### Phase 1: Backend OSS Infrastructure

- [x] 在 `backend/pom.xml` 添加 `aliyun-sdk-oss` (3.18.1) 和 `aliyun-java-sdk-sts` (3.1.2) 依赖
- [x] 验证 `mvn dependency:resolve` 通过
- [x] 新增 `backend/src/main/java/com/hillcommerce/modules/oss/config/OssProperties.java`，`@ConfigurationProperties("oss")` 配置类
- [x] 在 `backend/src/main/resources/application.yml` 顶层新增 `oss:` 配置（endpoint, bucket, oss-region, sts-region, access-key-id, access-key-secret, role-arn, upload-dir），均通过环境变量注入，其中 upload-dir 默认 `products/`
- [x] 新增 `backend/src/main/java/com/hillcommerce/modules/oss/dto/OssStsToken.java` record
- [x] 新增 `backend/src/main/java/com/hillcommerce/modules/oss/service/OssService.java`：
  - `generateStsToken()` 通过 `DefaultAcsClient` + `com.aliyuncs.sts.model.v20150401.AssumeRoleRequest` 获取 STS 临时凭证
  - STS Policy 限制 `PutObject` 到 `products/*` 前缀
  - 凭证有效期 1 小时
  - 配置未就绪时抛 `IllegalStateException("OSS not configured")`
  - 调用失败时 `log.error` + 包装异常
- [x] 编写 `OssServiceTest`（覆盖：配置未就绪抛异常、AssumeRole duration、Policy resource、返回字段映射；通过可 mock 的 STS client/factory 避免真实网络调用）
- [x] 新增 `backend/src/main/java/com/hillcommerce/modules/oss/web/OssController.java`：
  - `GET /api/admin/oss/sts` → 返回 `{accessKey, secretKey, securityToken, ossRegion, bucket, endpoint, uploadDir}`
- [x] 编写 `OssControllerWebMvcTest`（覆盖：ADMIN 可访问返回 200、SALES 可访问返回 200、未认证返回 401、非授权角色返回 403）
- [x] 确认 `SecurityConfig.java` 中的 `/api/admin/**` 规则已覆盖新端点

### Phase 2: Frontend Upload Tooling

- [x] `cd frontend/next-app && npm install ali-oss`
- [x] 如果项目还没有 Vitest，安装 `vitest @vitest/coverage-v8 jsdom @testing-library/react @testing-library/jest-dom`
- [x] 新增 `frontend/next-app/src/lib/upload/types.ts`：`OssStsToken`, `UploadState`, `UploadResult`, `ImageUploadMeta` 类型
- [x] 新增 `frontend/next-app/src/lib/upload/image-compress.ts`：
  - Canvas API 压缩，限制最大宽度 1920px，质量 0.8
  - 仅允许 `image/jpeg`, `image/png`, `image/webp`
  - 超过 maxSize（默认 10MB）直接拒绝
- [x] 编写 `image-compress.test.ts`（jsdom 环境，覆盖：格式校验、大小限制、压缩输出）
- [x] 新增 `frontend/next-app/src/lib/upload/oss-client.ts`：
  - `requestStsToken()` → `fetch /api/admin/oss/sts`，失败抛异常
  - `uploadToOss(blob, fileName, token)` → `ali-oss` SDK `client.put`，30s 超时，返回完整 HTTPS 图片 URL
- [x] 编写 `oss-client.test.ts`（mock fetch，覆盖：正常获取、401 失败）

### Phase 3: Upload Components

- [x] 新增 `frontend/next-app/src/features/admin/catalog/image-uploader.tsx`：
  - Props: `value`, `onChange`, `onUploadingChange(uploadingCount)`, `placeholder`, `suggestSize`, `maxSize`
  - 状态：empty → 虚线框点击上传；compressing → "压缩中..."；uploading → 进度条；uploaded → 缩略图预览 + 删除/更换；error → 错误提示 + 重试
- [x] `tsc --noEmit` 通过
- [x] 新增 `frontend/next-app/src/features/admin/catalog/images-uploader.tsx`：
  - Props: `value: ImageUploadMeta[]`, `onChange`, `onUploadingChange(uploadingCount)`, `maxCount`（默认 10）
  - 80×80 缩略图网格，左上角序号，右上角 × 删除
  - 支持批量选择文件
  - HTML5 drag-and-drop 排序，批量上传完成后统一更新 sortOrder
  - 末尾 + 按钮添加，上传中时 disabled
  - 底部显示总数 + 上传进度提示
- [x] 编写 `image-uploader.test.tsx`（覆盖：空态渲染、已有图片缩略图、删除操作）
- [x] 编写 `images-uploader.test.tsx`（覆盖：空态、多图渲染数量、删除回调）
- [x] `tsc --noEmit` 通过

### Phase 4: Product Editor Integration

- [x] 改造 `frontend/next-app/src/features/admin/catalog/product-editor.tsx`：
  - 封面图：`<input>` → `<ImageUploader>`
  - 详情图：文本 URL 列表 → `<ImagesUploader>`
  - 通过两个上传组件的 `onUploadingChange` 维护上传中数量，上传未完成时禁用保存按钮并显示“还有 N 张图片正在上传中”
  - 更新 section 描述文案
- [x] `tsc --noEmit` 通过

### Phase 5: Configuration & E2E

- [x] 在 `.env.example` 添加 OSS 环境变量占位；本地 `.env` 可按需添加真实值但不提交密钥（OSS_ENDPOINT, OSS_BUCKET, OSS_REGION, OSS_STS_REGION, OSS_ACCESS_KEY_ID, OSS_ACCESS_KEY_SECRET, OSS_STS_ROLE_ARN, OSS_UPLOAD_DIR）
- [x] 在 `docker-compose.yml` `backend` 服务 `environment` 中传递 OSS 环境变量
- [ ] 在阿里云 OSS 控制台配置商家后台域名的 CORS 规则和图片读策略/CDN 域名策略
- [ ] `docker compose --profile app up -d` 启动全栈
- [ ] 手动验证：打开 `http://localhost:3000/admin/products/new`，封面图区域为虚线框上传按钮（非文本输入）
- [ ] 手动验证：选择图片 → 压缩 → 上传 → 缩略图预览出现
- [ ] 手动验证：多张详情图上传、拖拽排序、删除
- [ ] 手动验证：保存商品后，列表页和详情页正确显示 OSS URL 图片
- [ ] 手动验证：缩略图通过 `?x-oss-process=image/resize,w_200` 按需生成
- [ ] 手动验证：上传过程中保存按钮置灰并显示“还有 N 张图片正在上传中”

## Dependencies

- Phase 1 完成后，前端 STS 请求（oss-client）才能调通
- Phase 2 完成后，Phase 3 的上传组件才有所需工具函数
- Phase 3 完成后，Phase 4 的 product-editor 改造才能集成
- Phase 1-4 全部完成后，Phase 5 端到端验证才有意义

## Suggested MVP Scope

全部实现（无额外可选模块）。

## Done When

- `GET /api/admin/oss/sts` 返回 STS 临时凭证（ADMIN/SALES 可访问，未认证 401，非授权角色 403）
- 后台商品编辑器封面图区域显示虚线框上传（替换文本输入）
- 详情图区域显示缩略图网格（替换文本 URL 列表）
- 选择图片后自动压缩并直传 OSS，上传完成显示缩略图
- 图片上传未完成时不能提交商品表单
- 多图支持拖拽排序和删除
- 列表页/详情页图片正常加载（完整 HTTPS OSS URL + OSS 缩略图参数）
- 数据库无新增列/表，现有 `cover_image_url` 和 `image_url` 列存完整 HTTPS OSS URL
- 水印功能不做
