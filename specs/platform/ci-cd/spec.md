# CI/CD Pipeline

## 概述

在现有 GitHub Actions CI 基础上新增 CD pipeline，实现 push main → 自动构建 + 部署到 VPS 的完整自动化。

## 技术选型

- **镜像仓库**：GitHub Container Registry (ghcr.io)，免费、无需额外账号
- **部署方式**：SSH 到 VPS，docker compose 方式运行容器
- **编排**：单份 GitHub Actions workflow，CI jobs + CD job 串联

## 镜像策略

- 命名：`ghcr.io/hhhhill/hill-commerce-backend:latest` / `ghcr.io/hhhhill/hill-commerce-frontend:latest`
- 策略：只打 `latest` tag，个人项目无需多版本/回滚
- 构建：Docker Buildx，复用 Maven/npm 层缓存

## GitHub Secrets（需手动配置）

| Secret | 说明 |
|--------|------|
| `VPS_HOST` | VPS 公网 IP 或域名 |
| `VPS_USER` | SSH 用户名 |
| `VPS_SSH_KEY` | SSH 私钥（对应 VPS 上 `authorized_keys`） |
| `GHCR_TOKEN` | GitHub Personal Access Token（写 packages 权限） |

## CD Job 流程

```
1. Checkout 代码
2. 登录 GHCR
3. Build + Push 后端镜像（maven skip tests，tests 已在 CI job 跑过）
4. Build + Push 前端镜像
5. SCP docker-compose.yml 到 VPS
6. SSH 执行：docker login + docker pull + docker compose up -d --remove-orphans
```

## VPS 准备（一次性）

1. 创建 deploy 用户，生成 SSH key pair，公钥写入 `authorized_keys`
2. 安装 Docker + Docker Compose
3. 创建 `/opt/hill-commerce/` 目录，放入 `.env`（OSS 密钥等，不入仓库）
4. 配置 GitHub Secrets

## 数据库迁移

后端使用 Flyway，容器启动时自动执行。无需额外步骤。

## 失败处理

- CI fail → CD 不执行
- CD 镜像构建/推送失败 → 不 SSH 部署
- 远端 docker compose 失败 → GitHub Actions 报红
- 不自动回滚，旧容器继续运行（`up -d` 失败不会停止现有容器）

## 影响范围

### 修改文件
- `.github/workflows/ci.yml` — 新增 CD job

### 新增文件
- 无额外文件，workflow 即所有变更
