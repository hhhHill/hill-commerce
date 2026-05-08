# hill-commerce

`hill-commerce` 是一个实体商品电商 MVP 项目，当前仓库已完成任务 1 的基础骨架搭建。

## 当前技术栈

- Backend: Java 21, Spring Boot 4, Spring Security 7, MyBatis-Plus, Flyway
- Frontend: Next.js 15, React 19, TypeScript, Tailwind CSS 4
- Database: MySQL 9.7 LTS
- Cache: Redis 8, 当前仅预留接入位
- Messaging: RocketMQ, 当前仅预留事件扩展位
- Deployment: Docker Compose, Nginx

## 目录结构

```text
backend/             Spring Boot 后端
frontend/next-app/   Next.js 前端
ops/                 部署相关配置
docs/                规格说明与实施计划
```

## 本地准备

建议环境：

- JDK 21
- Maven 3.9+
- Node.js 20+
- Docker Desktop

当前机器已安装更高版本 JDK，但 `pom.xml` 已固定以 Java 21 为目标版本。

## 环境变量

复制 `.env.example` 并根据本地环境调整。

重点变量：

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `CACHE_ENABLED`
- `ROCKETMQ_ENABLED`
- `ROCKETMQ_NAME_SERVER`

## 本地基础服务

仅启动数据库和 Redis：

```powershell
docker compose up -d mysql redis
```

如后续补齐 Dockerfile，可再启动完整应用 profile：

```powershell
docker compose --profile app up -d
```

## 当前完成项

- 后端最小启动骨架
- 前端最小启动骨架
- MySQL 与 Redis Compose 配置
- Nginx 反向代理占位
- 缓存服务抽象预留
- 领域事件发布抽象预留
- Flyway 首版数据库基线与种子数据

## 数据库基线说明

当前已提供：

- `V1__init_schema.sql`：MVP 首版核心表结构
- `V2__seed_roles.sql`：`CUSTOMER`、`SALES`、`ADMIN` 角色种子
- `V3__seed_admin_account.sql`：本地默认管理员种子账号

默认管理员种子信息：

- Email: `admin@hill-commerce.local`
- Bootstrap Password Source: `Admin@123456`

注意：

- 当前种子密码在任务 2 里采用引导用哈希策略落库。
- 任务 3 实现认证时，必须将运行期密码编码方案与该种子对齐，或在启动后立即重置管理员密码。

## 下一步

按实施计划进入：

- 任务 3：认证、会话与权限体系
