# hill-commerce

面向实体商品销售的现代化商城，覆盖前台交易与后台管理两条核心闭环，开箱即用。

## 项目亮点

- **完整电商双闭环** — 15 个 feature spec 覆盖全链路：注册登录 → 商品浏览 → 购物车 → 下单 → 支付 → 收货 → 售后，后台分类 → 商品管理 → 库存 → 发货 → 数据统计
- **现代化全栈技术** — Java 21 + Spring Boot 4 后端，Next.js 15.5 + React 19 + TypeScript 5.8 + Tailwind CSS 4 前端，MySQL 8.4 主库
- **智能推荐引擎** — 内置 Gorse 协同过滤推荐，首页个性化推荐 + 商品详情页相似推荐，用户浏览/购买行为实时反馈
- **OSS 图片直传** — 阿里云 OSS + STS 临时凭证，浏览器端 Canvas 压缩后直传，缩略图按需生成
- **一键全栈部署** — Docker Compose 编排 MySQL + Redis + Gorse + Backend + Frontend + Nginx，一条命令启动
- **类型安全全链路** — 后端 Java + 前端 TypeScript strict + 后端集成测试覆盖关键路径
- **默认管理员开箱即用** — 内置种子账号，启动即可登录后台

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21, Spring Boot 4, Spring Security 7, MyBatis-Plus, Flyway |
| 前端 | Next.js 15.5, React 19, TypeScript 5.8, Tailwind CSS 4, Recharts |
| 数据库 | MySQL 8.4 |
| 缓存 | Redis 8 |
| 消息队列 | RocketMQ（预留，非 MVP 主链路前提） |
| 推荐引擎 | Gorse |
| 对象存储 | 阿里云 OSS |
| 部署 | Docker Compose, Nginx |

## 快速部署

### 前置条件

只需要 **Docker Desktop**。

### 30 秒全栈启动

```powershell
git clone <repo-url> hill-commerce
cd hill-commerce
cp .env.example .env
docker compose --profile app up -d
```

### VPS / 生产环境部署

不要在小规格 VPS 上执行 `docker compose --profile app up -d --build`。当前后端镜像会在构建阶段运行 Maven 打包，Java 依赖下载、编译和镜像层写入会显著消耗 CPU、内存、磁盘 IO，并可能把 SSH 一起拖死。

推荐路径：

1. 由 GitHub Actions 构建并推送后端/前端镜像到 ACR。
2. VPS 上只执行 `pull + up --no-build`。

手动部署命令：

```bash
cd /opt/hill-commerce
echo "$ACR_PASSWORD" | docker login "$ACR_REGISTRY" -u "$ACR_USERNAME" --password-stdin
ACR_REGISTRY=<your-registry> ACR_NAMESPACE=<your-namespace> ops/scripts/deploy-prod.sh
```

如果只是重启应用层，不要追加 `--build`，直接用：

```bash
cd /opt/hill-commerce
ACR_REGISTRY=<your-registry> ACR_NAMESPACE=<your-namespace> ops/scripts/restart-app.sh
```

### 访问入口

| 服务 | 地址 |
|------|------|
| 前台 / 后台 | http://localhost |
| 后端 API | http://localhost:8080 |
| Mailhog（开发邮件） | http://localhost:8025 |

### 默认管理员

- 邮箱：`admin@hill-commerce.local`
- 密码：`Admin@123456`

### 开发模式（仅启基础依赖）

```powershell
# 启动 MySQL + Redis + Mailhog + Gorse
docker compose up -d

# 启动后端（另开终端）
mvn -pl backend spring-boot:run

# 启动前端（另开终端）
cd frontend/next-app
npm install
npm run dev
```

开发模式下前端运行在 http://localhost:3000，后端在 http://localhost:8080。

### 环境变量

复制 `.env.example` 并按需调整。常用变量：

`APP_PROFILE` `DB_HOST` `DB_PORT` `DB_NAME` `DB_USERNAME` `DB_PASSWORD` `REDIS_HOST` `REDIS_PORT` `MAIL_HOST` `MAIL_PORT` `OSS_ACCESS_KEY_ID` `OSS_ACCESS_KEY_SECRET` `OSS_ROLE_ARN` `OSS_REGION` `OSS_BUCKET` `CACHE_ENABLED` `ROCKETMQ_ENABLED`

## 验证

```powershell
# 后端测试
mvn -pl backend test

# 前端类型检查
cd frontend/next-app
npm run typecheck
```

## 仓库结构

```text
backend/               Spring Boot 后端（模块化单体）
frontend/next-app/     Next.js 前端
ops/                   Nginx 配置、Gorse 配置、MySQL 初始化脚本
specs/                 各 feature 的规范/计划/任务文档
.specify/              Spec Kit 工作流与项目治理
```

## 运行稳定性

生产/测试环境建议使用 `ops/systemd/` 下的 unit 文件托管整套 Compose 服务，并配合 `ops/scripts/` 下的脚本执行日常运维：

- `ops/scripts/status.sh`：查看主机资源、Compose 状态和健康检查
- `ops/scripts/restart-app.sh`：安全重启应用层容器
- `ops/scripts/deploy-prod.sh`：拉取预构建镜像并以 `--no-build` 方式更新整套服务
- `ops/scripts/collect-debug.sh`：收集主机与容器调试信息
- `ops/scripts/check-stack.sh`：供 systemd timer 周期执行的轻量巡检脚本

Nginx 已改为通过宿主机映射端口回源到前后端容器，避免在这台机器上继续依赖不稳定的 Docker DNS 解析。生产环境应避免在 VPS 上直接构建应用镜像，优先使用预构建镜像部署。

## 工程方法

本项目采用 **Spec-driven + Agent-assisted + Test-verified + Human-reviewed** 模式，规范定义在 `specs/<feature>/` 中，治理原则见 `.specify/memory/constitution.md`。

开发流程：阅读 spec → 实现 → 测试验证 → 人工审阅。agent 用于加速执行，不替代规范和评审。

## 数据库基线

Flyway 迁移（`backend/src/main/resources/db/migration/`）：

- `V1__init_schema.sql` — MVP 核心表结构
- `V2__seed_roles.sql` — CUSTOMER / SALES / ADMIN 角色种子
- `V3__seed_admin_account.sql` — 默认管理员账号
- `V4__migrate_admin_seed_password_to_bcrypt.sql` — 密码哈希迁移
