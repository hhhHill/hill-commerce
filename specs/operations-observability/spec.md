# Feature Specification: operations-observability

**Feature**: `operations-observability`  
**Status**: active  

## Purpose

定义登录日志、后台操作日志、商品浏览日志三类数据采集与查询能力，为运营和审计提供基础可观测性。

## Scope

### In Scope

- 登录日志：记录每次登录尝试（成功/失败）的时间、IP、UA
- 操作日志：记录 Admin/Sales 的写操作（增删改用户、商品、分类、发货）
- 商品浏览日志：记录前台用户浏览商品详情的动作
- 提供三类日志的后台查询 API
- 商品详情页浏览事件直连后端上报
- Admin 可查询所有三类日志
- Sales 可查询商品浏览日志

### Out of Scope

- 邮件通知与发送记录
- Dashboard 统计（已在 `admin-account-management` 实现）
- 停留时长采集
- 高级分析、大屏可视化、实时告警
- 复杂分页（MVP 默认最近 100 条）
- 后台日志列表页面与导航入口

## Roles

| 角色 | 权限 |
|------|------|
| Admin | 查看所有三类日志 |
| Sales | 查看商品浏览日志（不可查看登录日志和操作日志） |
| Customer | 无日志查看权限 |

## User Journeys

### Journey 1: Admin 查看登录日志

Admin 调用登录日志查询 API → 获取最近 100 条登录记录（邮箱、角色、成功/失败、IP、时间）→ 可按邮箱或结果筛选。

### Journey 2: Admin 查看操作日志

Admin 调用操作日志查询 API → 获取 Admin/Sales 的写操作记录（操作者、操作类型、对象、时间）→ 可按操作者或操作类型筛选。

### Journey 3: Admin/Sales 查看浏览日志

Admin 或 Sales 调用浏览日志查询 API → 获取用户在商城的商品浏览记录（用户/匿名标识、商品ID、分类ID、时间）→ 可按商品或分类筛选。

### Journey 4: 商品浏览自动采集

用户（登录或未登录）进入商品详情页 → 前端自动上报浏览事件到后端 → 后端写入 `product_view_logs`。

## Data Model Rules

### 登录日志

- 登录成功和失败分别记录，`login_result` 取值 `SUCCESS` / `FAILURE`
- 失败时 `user_id` 为 null，`role_snapshot` 为 `UNKNOWN`
- IP 优先取 `X-Forwarded-For` 最左端，回退 `request.getRemoteAddr()`
- 登录日志写入失败不得改变原始登录结果：成功登录仍返回成功，失败登录仍保持认证失败语义

### 操作日志

- 仅记录写操作（增/删/改），不记录读操作
- `action_type` 枚举：`CREATE_USER`、`DISABLE_USER`、`ENABLE_USER`、`RESET_PASSWORD`、`CREATE_CATEGORY`、`UPDATE_CATEGORY`、`DELETE_CATEGORY`、`CREATE_PRODUCT`、`UPDATE_PRODUCT`、`DELETE_PRODUCT`、`SHIP_ORDER`
- `target_type` 枚举：`USER`、`CATEGORY`、`PRODUCT`、`ORDER`
- 业务方法自身抛出的异常必须原样向上抛出，不得被日志切面吞掉
- 仅日志采集与写入过程中的异常可被降级处理；降级方式为 `log.error` 且不向上抛出

### 商品浏览日志

- 前端进入商品详情页时上报，不等待离开页面
- 登录用户由后端从认证上下文写入 `user_id`
- 未登录用户必须携带 `anonymous_id`
- `anonymous_id` 由前端首次访问时生成并持久化，同一浏览器在标识未清除前复用同一个值
- 接口为公开端点，无需鉴权
- 若匿名用户无法生成 `anonymous_id`，则前端不发起上报请求

## API Boundaries

| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| `GET` | `/api/admin/login-logs` | 查询登录日志，可选 `?email=` `?result=` | Admin |
| `GET` | `/api/admin/operation-logs` | 查询操作日志，可选 `?operatorUserId=` `?actionType=` | Admin |
| `GET` | `/api/admin/view-logs` | 查询浏览日志，可选 `?productId=` `?categoryId=` | Admin, Sales |
| `POST` | `/api/storefront/view-log` | 上报浏览事件 | 公开 |

## Business Rules

- 登录成功与失败都应有日志
- 后台关键写操作应有操作日志（通过 `@OperationLog` AOP 注解自动采集）
- 操作日志写入失败不得阻断业务主流程
- 日志查询默认返回最近 100 条，按时间倒序
- 商品浏览日志写入为异步非阻塞（前端 fire-and-forget）
- `/api/storefront/view-log` 必须允许匿名请求访问
- 三类日志查询接口统一返回 `items` 数组；MVP 不返回总数与分页元数据

## Acceptance Criteria

- Admin 可查询登录日志并按邮箱、结果筛选
- Admin 可查询操作日志并按操作者、操作类型筛选
- Admin/Sales 可查询商品浏览日志
- `/api/storefront/view-log` 对匿名请求可访问
- 用户浏览商品详情时自动上报并写入数据库
- 匿名用户浏览商品详情时可写入 `product_view_logs.anonymous_id`
- 登录事件自动写入数据库
- Admin/Sales 写操作自动写入操作日志
- 操作日志写入失败不阻断主业务
