# hill-commerce MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个面向实体商品的商城 MVP，完成从商品展示、购物车、下单、模拟支付到后台发货和基础审计的完整闭环。

**Architecture:** 项目采用模块化单体架构。后端使用 `Java 21 + Spring Boot 4 + Spring Security 7 + MyBatis-Plus + MySQL 9.7 + Flyway`，前端使用 `Next.js 15 + React 19 + TypeScript + Tailwind CSS 4`，前后台共用一个 Web 工程但在路由和权限上隔离。首版不做微服务、不做真实支付、不做高级推荐与分析；`Redis 8` 和 `RocketMQ` 只做架构预留，不作为主流程硬依赖。

**Tech Stack:** Java 21, Spring Boot 4, Spring Security 7, MyBatis-Plus, MySQL 9.7 LTS, Flyway, Redis 8, RocketMQ, Maven, Next.js 15, React 19, TypeScript, Tailwind CSS 4, Docker Compose, Nginx, JUnit 5, Spring Boot Test, Testcontainers

---

## 0. 计划说明

### 0.1 实施原则
- 按“先主链路、后增强”的顺序推进。
- 先完成数据库与认证骨架，再进入商品、订单、支付、发货链路。
- 每个任务结束后必须至少完成一次可验证的集成检查。
- 保持模块边界清晰，不将业务逻辑堆积到 Controller 或 Mapper。
- Redis 与 RocketMQ 首版只做抽象与部署预留，不将其绑定为交易链路前置条件。

### 0.2 建议目录结构

**后端**
- `backend/pom.xml`
- `backend/src/main/java/.../common`
- `backend/src/main/java/.../framework`
- `backend/src/main/java/.../framework/cache`
- `backend/src/main/java/.../framework/events`
- `backend/src/main/java/.../modules/user`
- `backend/src/main/java/.../modules/product`
- `backend/src/main/java/.../modules/cart`
- `backend/src/main/java/.../modules/order`
- `backend/src/main/java/.../modules/payment`
- `backend/src/main/java/.../modules/shipping`
- `backend/src/main/java/.../modules/admin`
- `backend/src/main/java/.../modules/audit`
- `backend/src/main/java/.../modules/notification`
- `backend/src/main/resources/db/migration`

**前端**
- `frontend/next-app/package.json`
- `frontend/next-app/src/app`
- `frontend/next-app/src/components`
- `frontend/next-app/src/features/shop`
- `frontend/next-app/src/features/account`
- `frontend/next-app/src/features/cart`
- `frontend/next-app/src/features/order`
- `frontend/next-app/src/features/admin`
- `frontend/next-app/src/lib`

**部署与文档**
- `docker-compose.yml`
- `ops/nginx/`
- `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md`
- `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md`

### 0.3 任务依赖图
- 任务 1 是所有后续任务前置。
- 任务 2、任务 3 可在任务 1 后并行。
- 任务 4 依赖任务 3。
- 任务 5 依赖任务 2、任务 4。
- 任务 6 依赖任务 3、任务 5。
- 任务 7 依赖任务 6。
- 任务 8 依赖任务 7。
- 任务 9 可在任务 2、任务 6、任务 8 基础上穿插推进。
- 任务 10 最后进行。
- Redis 与 RocketMQ 的接口预留应在任务 1 和任务 3 中完成基础框架，在任务 10 中确认扩展可行性。

---

## 1. 文件结构映射

### 1.1 后端关键职责落位
- `common`：通用枚举、响应模型、异常、工具
- `framework/config`：Spring Boot、MyBatis-Plus、Flyway、Mail、CORS、Jackson 等配置
- `framework/security`：登录、角色、Session/Cookie、安全过滤
- `framework/cache`：缓存接口、Key 规范、Redis 适配器预留
- `framework/events`：领域事件模型、发布器接口、本地发布器、RocketMQ 扩展位
- `modules/user`：注册、登录、用户、角色、地址
- `modules/product`：分类、SPU、SKU、销售属性、展示属性、图片
- `modules/cart`：购物车与结算前校验
- `modules/order`：订单创建、订单项快照、取消、超时关闭、确认收货、自动完成
- `modules/payment`：模拟支付、支付流水
- `modules/shipping`：发货、物流信息
- `modules/admin`：后台角色管理、基础统计聚合入口
- `modules/audit`：登录日志、操作日志、浏览日志
- `modules/notification`：邮件发送和发送日志

### 1.2 前端关键职责落位
- `src/app/(shop)`：前台页面
- `src/app/(auth)`：登录注册
- `src/app/admin`：后台页面
- `src/features/shop`：商品浏览与详情
- `src/features/cart`：购物车与结算
- `src/features/order`：订单查询与详情
- `src/features/account`：用户资料与地址
- `src/features/admin`：后台分类、商品、订单、发货、日志、统计
- `src/lib/http`：请求封装
- `src/lib/auth`：会话态与路由守卫
- `src/lib/config`：缓存与消息队列相关配置占位说明

---

## 2. 实施任务

### 任务 1：工程初始化与基础设施

**目标**
- 建立前后端工程、统一目录结构、环境配置与本地运行基线。

**涉及文件**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/.../Application.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/resources/application-prod.yml`
- Create: `backend/src/main/java/.../framework/cache/...`
- Create: `backend/src/main/java/.../framework/events/...`
- Create: `frontend/next-app/package.json`
- Create: `frontend/next-app/src/app/layout.tsx`
- Create: `frontend/next-app/src/app/page.tsx`
- Create: `docker-compose.yml`
- Create: `ops/nginx/default.conf`
- Create: `README.md`

- [ ] 明确后端包结构、前端目录结构、环境变量命名规范
- [ ] 初始化 Spring Boot 4 后端工程
- [ ] 初始化 Next.js 15 前端工程
- [ ] 配置 MySQL、应用、前端的本地开发环境变量
- [ ] 在配置层预留 Redis 8 和 RocketMQ 的环境变量与开关
- [ ] 建立缓存服务抽象和领域事件发布抽象的基础骨架
- [ ] 建立 Docker Compose 基础服务：MySQL、后端、前端、Nginx 占位
- [ ] 在 Docker Compose 中预留 Redis 服务位，RocketMQ 先写明后续接入策略
- [ ] 补充项目启动说明和约定文档

**交付物**
- 可启动的前后端空骨架
- 本地开发环境说明
- 容器化基础编排文件
- 缓存与事件扩展接口骨架

**验收标准**
- 后端可启动并返回健康检查
- 前端可启动并访问首页
- MySQL 可通过配置连接
- 基础目录结构与规格说明一致
- Redis 与 RocketMQ 的配置占位清晰，但系统在未启用它们时仍可运行

### 任务 2：数据库基线与迁移体系

**目标**
- 将规格文档中的核心表结构固化为可迁移、可演进的数据库基线。

**涉及文件**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/resources/db/migration/V2__seed_roles.sql`
- Create: `backend/src/main/resources/db/migration/V3__seed_admin_account.sql`
- Create: `backend/src/test/.../migration/`

- [ ] 根据规格文档创建首版数据库表迁移
- [ ] 加入唯一索引、外键、状态字段与逻辑删除字段
- [ ] 初始化 `CUSTOMER`、`SALES`、`ADMIN` 角色
- [ ] 预留一个本地默认管理员账号种子方案
- [ ] 建立迁移执行与回归检查方式

**交付物**
- 可重复执行的迁移脚本
- 初始角色数据
- 管理员种子策略

**验收标准**
- 新库执行 Flyway 后可生成全部首版表
- 角色数据存在且唯一
- 表字段与规格说明一致
- 数据库基线可用于后续模块开发

### 任务 3：认证、会话与权限体系

**目标**
- 建立统一账号体系、Session/Cookie 登录态与角色鉴权。

**涉及文件**
- Create: `backend/src/main/java/.../framework/security/...`
- Create: `backend/src/main/java/.../modules/user/...`
- Create: `frontend/next-app/src/app/(auth)/login/page.tsx`
- Create: `frontend/next-app/src/app/(auth)/register/page.tsx`
- Create: `frontend/next-app/src/lib/auth/...`

- [ ] 实现邮箱注册、邮箱唯一校验、邮箱密码登录、注销
- [ ] 建立 `users / roles / user_roles` 的服务与查询链路
- [ ] 配置 Spring Security 会话认证
- [ ] 定义前台用户和后台路由的权限边界
- [ ] 保证认证流程不依赖 Redis 才能工作
- [ ] 实现前端登录、注册、会话态读取与路由拦截
- [ ] 写入登录成功/失败日志

**实现约束**
- 后端继续作为唯一认证事实源，前端不落地 token。
- 采用 Session Cookie 维持登录态；前端通过后端 `GET /api/auth/me` 获取当前用户。
- 前端浏览器通过 Next.js Route Handlers 代理认证请求，确保 `JSESSIONID` 作为前端同源 Cookie 被 middleware 与 Server Components 读取。
- `middleware.ts` 仅做基于 Cookie 存在性的快速拦截，不在 middleware 中调用后端接口。
- 精细角色校验在 Next.js Server Components 中完成。
- 任务 3 通过 `/account` 与 `/admin` 两个最小受保护页面承接前台与后台验收。

**执行步骤**
- [ ] 后端补齐认证集成测试：覆盖注册、登录成功、登录失败、注销、Customer 禁止访问后台、Admin 可访问后台
- [ ] 后端补齐登录成功/失败日志，日志字段满足 spec 约束
- [ ] 保持 `AuthController`、`SecurityConfig`、`UserAccountService` 的会话认证与角色边界实现一致
- [ ] 新增前端认证基础配置与类型：后端地址、会话用户类型、服务端鉴权辅助函数、浏览器认证请求函数
- [ ] 新增 `frontend/next-app/middleware.ts`：保护 `/account`、`/admin`，并阻止已登录用户再次访问 `/login`、`/register`
- [ ] 新增登录页与注册页，完成表单提交、错误提示、成功跳转
- [ ] 新增 `/account` 页面并在服务端要求登录
- [ ] 新增 `/admin` 页面并在服务端要求 `ADMIN` 或 `SALES` 角色
- [ ] 新增 `/forbidden` 页面承接无权限跳转
- [ ] 轻量更新首页，提供登录/注册/受保护页面入口用于人工验证
- [ ] 运行后端测试、前端 typecheck、前端 build，确认任务 3 验收路径成立

**交付物**
- 可用的注册登录闭环
- 角色鉴权基础框架
- 登录日志能力

**验收标准**
- Customer 可注册、登录、注销
- Sales/Admin 可登录后台
- 未登录用户无法访问购物车和结算
- 无权限用户无法访问后台受限路由
- 登录成功与失败均产生日志

### 任务 4：商品分类、SPU、SKU 后台管理

**目标**
- 完成后台商品体系，让 Sales 能维护可售商品。

**涉及文件**
- Create: `backend/src/main/java/.../modules/product/...`
- Create: `frontend/next-app/src/app/admin/categories/page.tsx`
- Create: `frontend/next-app/src/app/admin/products/page.tsx`
- Create: `frontend/next-app/src/app/admin/products/new/page.tsx`
- Create: `frontend/next-app/src/app/admin/products/[id]/page.tsx`
- Create: `frontend/next-app/src/features/admin/product/...`

- [ ] 实现一级分类 CRUD
- [ ] 实现 SPU 基础信息管理
- [ ] 实现销售属性、展示属性和 SKU 维护
- [ ] 实现商品图片管理与富文本描述录入
- [ ] 实现商品上下架与逻辑删除
- [ ] 实现 SKU 自动编码生成和允许手工改码
- [ ] 实现每个 SKU 的低库存阈值配置

**交付物**
- 商品后台管理闭环
- 商品数据模型与页面管理能力

**验收标准**
- Sales 可创建分类、商品、SKU
- 每个商品最多配置 2 个销售属性
- 商品支持封面图和详情图
- 商品支持富文本描述
- 商品上架、下架、逻辑删除有效
- SKU 独立价格、库存、阈值可维护

### 任务 5：前台商品浏览与搜索

**目标**
- 提供访客可访问的商品浏览体验。

**涉及文件**
- Create: `frontend/next-app/src/app/(shop)/page.tsx`
- Create: `frontend/next-app/src/app/(shop)/products/page.tsx`
- Create: `frontend/next-app/src/app/(shop)/products/[id]/page.tsx`
- Create: `frontend/next-app/src/features/shop/...`
- Modify: 商品查询接口与前台查询 DTO

- [ ] 实现首页基础商品展示
- [ ] 实现分类商品列表
- [ ] 实现商品名称搜索
- [ ] 实现商品详情页
- [ ] 展示最低 SKU 价格起
- [ ] 选择 SKU 后刷新实际价格与库存
- [ ] 接入商品浏览日志记录

**交付物**
- 前台商品浏览页面
- 商品详情交互
- 搜索与浏览日志基础能力

**验收标准**
- 访客可浏览分类和商品详情
- 前台支持按商品名称搜索
- 商品详情显示图片、富文本描述、展示属性、销售属性
- SKU 选择后价格与库存变化正确
- 商品浏览日志可落库

### 任务 6：购物车与地址管理

**目标**
- 让登录用户具备加购与收货信息维护能力。

**涉及文件**
- Create: `backend/src/main/java/.../modules/cart/...`
- Create: `backend/src/main/java/.../modules/user/address/...`
- Create: `frontend/next-app/src/app/(shop)/cart/page.tsx`
- Create: `frontend/next-app/src/app/(shop)/account/addresses/page.tsx`
- Create: `frontend/next-app/src/features/cart/...`
- Create: `frontend/next-app/src/features/account/address/...`

- [ ] 实现购物车增删改查
- [ ] 保证同一购物车内同一 SKU 唯一
- [ ] 实现多地址管理和默认地址
- [ ] 对下架商品、禁用 SKU、库存不足场景做购物车侧提示
- [ ] 提供结算前购物车勾选与汇总能力

**交付物**
- 购物车闭环
- 地址管理闭环

**验收标准**
- 登录用户可加入购物车并修改数量
- 相同 SKU 不会在购物车中重复生成多条
- 用户可新增、编辑、删除地址并设置默认地址
- 失效商品或 SKU 在购物车中可被识别

### 任务 7：下单、库存扣减与订单快照

**目标**
- 完成结算与下单闭环，确保库存和订单数据一致。

**涉及文件**
- Create: `backend/src/main/java/.../modules/order/...`
- Create: `frontend/next-app/src/app/(shop)/checkout/page.tsx`
- Create: `frontend/next-app/src/app/(shop)/order-success/page.tsx`
- Create: `frontend/next-app/src/features/order/checkout/...`

- [ ] 实现结算预览接口
- [ ] 在下单前再次校验商品、SKU、库存和地址
- [ ] 创建订单、订单项和状态历史
- [ ] 按“下单即扣减库存”更新 SKU 库存
- [ ] 保存订单地址快照和商品快照
- [ ] 支持用户主动取消未支付订单并回补库存
- [ ] 在订单创建、取消处发布领域事件接口调用

**交付物**
- 稳定的下单服务
- 库存扣减与订单快照逻辑

**验收标准**
- 创建订单后立即扣减库存
- 订单项保存商品名、SKU 编码、属性文本、图片快照
- 只有 `PENDING_PAYMENT` 订单可取消
- 取消未支付订单后库存回补正确

### 任务 8：模拟支付与超时关闭

**目标**
- 打通支付闭环并处理超时未支付订单。

**涉及文件**
- Create: `backend/src/main/java/.../modules/payment/...`
- Create: `backend/src/main/java/.../modules/order/job/...`
- Create: `frontend/next-app/src/features/order/payment/...`

- [ ] 实现模拟支付接口和支付流水记录
- [ ] 支付成功后推动订单转为 `PAID`
- [ ] 实现 30 分钟未支付订单自动关闭任务
- [ ] 在自动关闭时回补库存并记录状态历史
- [ ] 处理重复支付、重复关闭的幂等问题
- [ ] 在支付成功和超时关闭处发布领域事件接口调用

**交付物**
- 模拟支付能力
- 订单自动关闭能力

**验收标准**
- 用户可完成模拟支付
- 支付成功后订单状态变为 `PAID`
- 超时订单自动变为 `CANCELLED`
- 超时取消后库存回补正确
- 支付和关闭流程具备基本幂等性

### 任务 9：发货、物流、确认收货与自动完成

**目标**
- 完成履约链路，让订单从已支付走到完成。

**涉及文件**
- Create: `backend/src/main/java/.../modules/shipping/...`
- Create: `frontend/next-app/src/app/(shop)/orders/page.tsx`
- Create: `frontend/next-app/src/app/(shop)/orders/[id]/page.tsx`
- Create: `frontend/next-app/src/app/admin/orders/page.tsx`
- Create: `frontend/next-app/src/app/admin/orders/[id]/page.tsx`

- [ ] 实现后台订单列表和订单详情
- [ ] 实现 Sales 发货录入：快递公司、运单号
- [ ] 发货后推动订单转为 `SHIPPED`
- [ ] 实现用户订单列表、详情与物流展示
- [ ] 实现用户确认收货
- [ ] 实现发货后 10 天自动完成任务
- [ ] 在发货、确认收货、自动完成处发布领域事件接口调用

**交付物**
- 履约后台
- 用户订单查询与收货能力
- 自动完成任务

**验收标准**
- Sales 可发货并录入物流信息
- 用户可查看订单和物流信息
- 用户可确认收货
- 已发货订单满 10 天可自动完成

### 任务 10：日志、邮件、基础统计与后台审计

**目标**
- 完成最小可用的运维与管理视角。

**涉及文件**
- Create: `backend/src/main/java/.../modules/audit/...`
- Create: `backend/src/main/java/.../modules/notification/...`
- Create: `backend/src/main/java/.../modules/admin/dashboard/...`
- Create: `frontend/next-app/src/app/admin/dashboard/page.tsx`
- Create: `frontend/next-app/src/app/admin/logins/page.tsx`
- Create: `frontend/next-app/src/app/admin/operations/page.tsx`

- [ ] 实现后台关键写操作的操作日志
- [ ] 完善商品浏览日志接入
- [ ] 实现邮件通知：下单成功、支付成功、发货通知
- [ ] 记录邮件发送结果
- [ ] 实现 Dashboard 基础统计聚合
- [ ] 实现登录日志、操作日志、浏览日志查询页
- [ ] 为后续 Redis 热门商品缓存和 RocketMQ 异步通知预留接入点说明

**交付物**
- 审计能力
- 基础通知能力
- 管理统计视图

**验收标准**
- 登录、操作、浏览日志可查询
- 关键邮件通知可触发
- 邮件失败不阻断交易流程
- Dashboard 显示商品数、订单状态数、销售额、注册用户数

### 任务 11：联调、异常处理与上线准备

**目标**
- 完成完整演示链路与可上线最小条件。

**涉及文件**
- Modify: 全局异常处理、错误页、部署配置、README
- Create: `frontend/next-app/src/app/not-found.tsx`
- Create: `frontend/next-app/src/app/error.tsx`
- Create: `ops/README.md`

- [ ] 完成前后端接口联调
- [ ] 补充 403/404/500 页面
- [ ] 梳理关键异常提示与表单校验提示
- [ ] 完成部署说明和运行脚本整理
- [ ] 做一轮完整业务演示走查
- [ ] 检查在未启用 Redis/RocketMQ 时系统可稳定运行
- [ ] 检查缓存抽象和事件抽象可被后续实现替换

**交付物**
- 可部署版本
- 演示脚本与说明

**验收标准**
- 可完整演示“浏览 -> 注册 -> 加购 -> 下单 -> 支付 -> 发货 -> 收货”
- 常见异常路径有清晰提示
- 部署步骤可重复执行
- Redis/RocketMQ 未启用时不影响 MVP 主链路

---

## 3. 测试与验证策略

### 3.1 后端验证
- 单元测试：订单状态流转、库存扣减/回补、权限判断
- 集成测试：注册登录、商品创建、下单、支付、发货、收货、超时任务
- 数据库测试：Flyway 迁移、关键唯一约束、逻辑删除语义

### 3.2 前端验证
- 页面可访问性检查
- 关键表单校验：注册、登录、地址、商品编辑、发货
- 主流程手工回归：浏览、加购、下单、支付、查单、发货

### 3.3 全链路验收场景
- 场景 1：访客浏览与搜索商品
- 场景 2：注册登录并维护地址
- 场景 3：加购、下单、模拟支付
- 场景 4：后台发货、前台查单、确认收货
- 场景 5：未支付订单超时关闭与回补库存
- 场景 6：发货 10 天后自动完成

---

## 4. 风险与控制点

### 4.1 高风险点
- 下单即扣库存导致的并发一致性问题
- 未支付超时关闭与用户主动支付的竞态
- SKU 组合数据和价格库存的一致性维护
- 前后台共用登录态时的权限边界
- 过早把缓存和消息队列绑定进主流程导致系统复杂度失控

### 4.2 控制策略
- 订单创建、支付、取消、发货都必须做状态前置校验
- 关键状态变更必须写入 `order_status_histories`
- 下单、取消、超时关闭流程必须考虑幂等
- 前端所有结算前数据仅作展示，最终以服务端校验为准
- 缓存只做加速层，不做首版唯一数据源
- 事件发布先走本地实现，RocketMQ 后续替换时不修改业务服务边界

---

## 5. 里程碑建议

### 里程碑 A：可登录可建商品
- 完成任务 1 至任务 4

### 里程碑 B：可下单可支付
- 完成任务 5 至任务 8

### 里程碑 C：可发货可审计
- 完成任务 9 至任务 10

### 里程碑 D：可演示可部署
- 完成任务 11

---

## 6. 完成定义

满足以下条件视为实施完成：
- 规格文档中 MVP 范围内功能全部具备可操作页面或接口
- 前台和后台主流程都可走通
- 数据库迁移、日志、邮件、统计具备最小可用性
- 系统可在本地和演示环境稳定运行
