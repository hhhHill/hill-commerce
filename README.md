# hill-commerce

`hill-commerce` 是一个面向实体商品销售的商城 MVP 项目，采用 `Spec-driven + Agent-assisted + Test-verified + Human-reviewed` 的工程模式推进开发。

项目目标不是一次性堆满所有电商能力，而是先稳定打通两条核心闭环：

- 前台交易闭环：注册登录、商品浏览、购物车、下单、模拟支付、查单、收货
- 后台管理闭环：分类管理、商品管理、库存维护、订单发货、日志与基础统计

## 项目定位

这是一个前后端分离的商城仓库：

- Backend：Java 21 + Spring Boot 4 的模块化单体
- Frontend：Next.js 15 + React 19 的 Web 应用
- Data：MySQL 为主库，Flyway 管理迁移
- Infra Reserve：Redis 与 RocketMQ 仅作为扩展预留，不是 MVP 主链路前提

当前仓库已经具备一部分可运行能力，而不只是空白脚手架：

- 后端已落地认证、角色边界、商品管理、基础健康检查与 Flyway 基线
- 前端已落地登录注册、前台商品浏览、购物车页骨架、后台分类与商品管理页面
- 测试已覆盖认证、商品后台、商品浏览与购物车准备等关键切片

## 工程方法

### 1. Spec-driven

本项目把 `specs/*` 作为规范事实源，先定义产品行为、边界和验收标准，再推进实现。

规范阅读顺序：

1. `.specify/memory/constitution.md`
2. `specs/<feature>/spec.md`
3. `specs/<feature>/plan.md`
4. `specs/<feature>/tasks.md`

当前商城 MVP 的 canonical baseline 位于：

- `specs/hill-commerce-mvp/spec.md`
- `specs/hill-commerce-mvp/plan.md`
- `specs/hill-commerce-mvp/tasks.md`

第一轮已拆分或正在拆分的 feature 包括：

- `auth-permission`
- `admin-product-management`
- `cart-preparation`
- `order-checkout`
- `payment`
- `fulfillment`
- `operations-observability`
- `product-discovery`

### 2. Agent-assisted

本仓库内置了面向 agent 协作的工作约束与 Spec Kit 流程，agent 不是替代规范和评审，而是加速规范执行、代码实现、测试补齐和文档维护。

仓库中的 agent 工作遵循这些原则：

- 先读 constitution、spec、plan、tasks，再实施中高复杂度工作
- README 负责开发入口说明，不负责重新定义业务真相
- 高风险改动必须能追溯到规范，而不是只依据即时对话或代码猜测
- 规范、实现、验证结果发生冲突时，先记录并由人工决策，不制造双真相

### 3. Test-verified

本项目要求关键实现通过可执行验证，而不是只依赖描述性说明。

当前验证手段包括：

- Backend：JUnit 5、Spring Boot Test、Spring Security Test、Testcontainers
- Frontend：TypeScript typecheck
- Manual verification：部分 feature 在 `specs/<feature>/manual-verification.md` 中补充人工验证步骤

这意味着本项目强调：

- 功能变更需要测试或等价验证证据
- 文档声称完成的能力，需要能被命令或手工流程复核
- 在宣称“已完成”前，应先跑完对应验证

### 4. Human-reviewed

agent 可以生成方案、修改代码、补测试、更新文档，但最终项目质量门槛仍由人来把关。

这里的 `Human-reviewed` 不是口号，而是交付原则：

- 规范冲突由人工裁决
- 高风险业务变更不能只凭 agent 直接定稿
- 重要实现应经过人工审阅代码、行为和验证结果
- README、规范和代码需要保持一致，避免 agent 自行扩写出第二套事实

## 技术栈

- Backend: Java 21, Spring Boot 4, Spring Security 7, MyBatis-Plus, Flyway
- Frontend: Next.js 15, React 19, TypeScript, Tailwind CSS 4
- Database: MySQL 9.7 LTS
- Cache: Redis 8
- Messaging: RocketMQ
- Deployment: Docker Compose, Nginx

## 仓库结构

```text
backend/             Spring Boot 后端
frontend/next-app/   Next.js 前端
ops/                 Nginx 与部署配置
specs/               Canonical specs、plans、tasks
.specify/            Spec Kit 工作流与项目治理记忆
docs/                历史和辅助文档
```

## 本地准备

建议环境：

- JDK 21
- Maven 3.9+
- Node.js 20+
- Docker Desktop

## 环境变量

复制 `.env.example` 并按本地环境调整。

重点变量：

- `APP_PROFILE`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `CACHE_ENABLED`
- `ROCKETMQ_ENABLED`
- `MAIL_HOST`
- `MAIL_PORT`

## 本地启动

仅启动基础依赖：

```powershell
docker compose up -d mysql redis mailhog
```

启动后端：

```powershell
mvn -pl backend spring-boot:run
```

启动前端：

```powershell
cd frontend/next-app
npm install
npm run dev
```

如需要用 Compose 启动完整应用栈：

```powershell
docker compose --profile app up -d --build
```

## 验证命令

后端测试：

```powershell
mvn -pl backend test
```

前端类型检查：

```powershell
cd frontend/next-app
npm run typecheck
```

## 数据库基线

当前已提供的 Flyway 迁移包括：

- `V1__init_schema.sql`：MVP 首版核心表结构
- `V2__seed_roles.sql`：`CUSTOMER`、`SALES`、`ADMIN` 角色种子
- `V3__seed_admin_account.sql`：默认管理员种子账号
- `V4__migrate_admin_seed_password_to_bcrypt.sql`：管理员密码哈希迁移

默认管理员种子信息：

- Email: `admin@hill-commerce.local`
- Bootstrap Password Source: `Admin@123456`

## 当前开发共识

如果你要继续在这个仓库里开发，默认遵循以下共识：

- 先看 spec，再改代码
- 先有验证，再宣称完成
- 先经人工审阅，再视为稳定结果
- agent 用来加速交付，不用来绕过规范和评审
