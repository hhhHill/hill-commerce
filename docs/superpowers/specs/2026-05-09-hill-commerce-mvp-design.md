# hill-commerce MVP 需求规格说明书

## 1. 项目简介

### 1.1 项目名称
`hill-commerce`

### 1.2 项目定位
`hill-commerce` 是一个面向实体商品销售的电子商务网站 MVP 项目，目标是在第一阶段快速交付一个可运行、可演示、可扩展的基础商城系统。

### 1.3 MVP 目标
MVP 第一版聚焦完成最核心的交易闭环与后台管理闭环：

- 用户注册、登录、浏览商品
- 登录用户加入购物车、提交订单、模拟支付
- 销售人员管理商品、库存、订单、发货
- 管理员管理销售账号、查看日志与基础统计
- 系统记录关键业务日志，为后续推荐、分析、报表扩展打基础

### 1.4 MVP 设计原则
- 不过度设计，优先保证主链路完整
- 先支持实体商品，不支持数字商品和服务商品
- 架构采用模块化单体，便于首版快速交付
- 支付、推荐、分析先做简化版或预留扩展点
- 所有设计以后续接真实支付、推荐系统和数据分析为前提，但首版不提前实现复杂功能
- Redis 和 MQ 在架构上预留接入点，但不作为 MVP 主链路运行前提

---

## 2. 用户角色

### 2.1 Customer
普通消费者，使用前台商城完成浏览、下单、支付和订单查看。

权限范围：
- 注册
- 登录 / 注销
- 浏览商品分类、商品列表、商品详情
- 按商品名称搜索
- 加入购物车、修改购物车数量、删除购物车项
- 管理收货地址
- 提交订单
- 模拟支付
- 取消未支付订单
- 查看订单列表、订单详情、物流信息
- 确认收货

### 2.2 Sales
销售人员，使用后台管理商品、库存和订单发货。

权限范围：
- 登录后台
- 管理一级分类
- 管理商品 SPU
- 管理 SKU、价格、库存、低库存阈值、上架状态
- 查看全部订单
- 查看订单详情
- 录入快递公司、运单号
- 更新发货状态
- 查看基础商品、订单、浏览数据

### 2.3 Admin
管理员，负责后台账号、审计和统计。

权限范围：
- 登录后台
- 管理 Sales 角色账号
- 重置 Sales 密码
- 查看登录日志
- 查看操作日志
- 查看基础统计数据

### 2.4 账号体系设计
MVP 采用统一账号体系：

- 所有角色共用 `users` 表
- 通过 `roles` 和 `user_roles` 进行角色区分
- 不单独拆分 `sales_accounts`、`admin_accounts`

---

## 3. MVP 功能范围

### 3.1 用户与认证
- 邮箱注册
- 邮箱唯一性实时校验
- 邮箱 + 密码登录
- 注销
- Session/Cookie 登录态维护
- 基于角色的访问控制
- 邮箱注册后首版不支持修改邮箱

### 3.2 商品与分类
- 一级分类管理
- 商品采用 `SPU + SKU` 模型
- 每个商品最多支持 2 个销售属性
- 展示属性采用简单 key-value
- 商品支持：
  - 1 张封面图
  - 多张详情图
  - 富文本描述
- 前台商品详情默认展示“最低 SKU 价格起”
- 后台可上架、下架、逻辑删除商品
- SKU 编码系统自动生成，允许手工修改
- 每个 SKU 维护独立：
  - 价格
  - 库存
  - 低库存预警阈值
  - 状态

### 3.3 商品浏览与搜索
- 未登录用户可浏览商品
- 前台按商品名称搜索
- 后台按商品名称 / SKU 编码搜索商品

### 3.4 购物车
- 仅登录用户可加入购物车
- 不支持匿名购物车
- 不支持登录后购物车合并
- 购物车项以 `sku_id` 为核心
- 支持修改数量、删除条目、勾选结算

### 3.5 地址管理
- 用户可维护多个收货地址
- 支持默认地址
- 地址字段包括：
  - 收货人
  - 手机号
  - 省
  - 市
  - 区
  - 详细地址
  - 邮编
- 下单时将地址写入订单快照

### 3.6 下单与库存
- 从购物车发起结算
- 提交订单前再次校验：
  - 商品是否上架
  - SKU 是否可售
  - 库存是否充足
- 库存采用“下单即扣减”
- 订单创建成功后立即扣减库存
- 未支付订单允许用户手动取消
- 未支付订单 30 分钟自动关闭并回补库存

### 3.7 支付
- 首版使用模拟支付
- 支付结构预留未来真实支付接入点
- 记录支付流水
- 支付成功后订单进入待发货状态

### 3.8 订单与物流
订单状态定义：
- `PENDING_PAYMENT`
- `CANCELLED`
- `PAID`
- `SHIPPED`
- `COMPLETED`

订单流转规则：
- 下单成功后：`PENDING_PAYMENT`
- 30 分钟未支付：`CANCELLED`
- 支付成功后：`PAID`
- Sales 发货后：`SHIPPED`
- 用户确认收货后：`COMPLETED`
- 发货后 10 天未确认：系统自动 `COMPLETED`

物流范围：
- 后台录入快递公司
- 后台录入运单号
- 前台展示物流信息
- 首版不接真实物流接口

### 3.9 邮件通知
支持基础邮件通知：
- 下单成功
- 支付成功
- 发货通知

要求：
- 发送记录可追踪
- 发送失败不影响主交易流程

### 3.10 后台统计
首版仅支持基础统计：
- 商品总数
- 上架商品数
- 低库存商品数
- 各订单状态数量
- 订单数
- 销售额
- 注册用户数

### 3.11 审计与日志
- 登录日志
- 后台操作日志
- 商品浏览日志

### 3.12 缓存与消息队列预留
- 技术方案正式纳入 `Redis 8`
- 后续事件驱动方案预留 `RocketMQ`
- MVP 首版主流程不得依赖 Redis 或 RocketMQ 才能运行
- 首版仅要求：
  - 预留缓存服务抽象
  - 预留领域事件发布抽象
  - 预留本地开发和部署扩展位

Redis 预留用途：
- 热门商品缓存
- 商品详情缓存
- 简单限流
- 会话扩展能力预留

RocketMQ 预留用途：
- 订单创建事件
- 订单取消事件
- 支付成功事件
- 发货事件
- 邮件异步通知事件
- 埋点与统计异步消费事件

---

## 4. 暂不实现功能

以下功能明确不在 MVP 范围内：

- 数字商品
- 服务类商品
- 预约 / 交付流程
- 真实支付渠道接入
- 在线退款
- 售后流程
- 多级分类
- 复杂筛选
- 拼音搜索 / 同义词搜索 / 全文检索
- Elasticsearch
- 匿名购物车
- 登录后购物车合并
- 推荐系统正式版
- 协同过滤正式实现
- 用户画像
- 趋势预测
- 销售异常检测
- 实时行为分析
- 精确停留时长统计
- 高级报表和可视化大屏
- 数据导入 / 导出
- 反爬虫
- 移动端 App
- 销售数据隔离与分单机制
- 支付后未发货自动催单 / 升级治理

---

## 5. 页面清单

### 5.1 前台页面
- 首页
- 分类商品列表页
- 商品详情页
- 登录页
- 注册页
- 购物车页
- 结算页
- 下单成功页
- 我的订单列表页
- 订单详情页
- 收货地址管理页
- 个人中心基础页

### 5.2 后台页面
- 后台登录页
- Dashboard 页
- 分类管理页
- 商品列表管理页
- 商品新增页
- 商品编辑页
- SKU 管理区块
- 订单列表页
- 订单详情页
- 发货处理页
- 用户角色管理页
- 登录日志页
- 操作日志页
- 基础统计页

### 5.3 通用异常页
- 403
- 404
- 500

### 5.4 列表页默认展示字段
前台订单列表：
- 订单号
- 下单时间
- 总金额
- 订单状态
- 商品缩略信息

后台订单列表：
- 订单号
- 用户邮箱
- 下单时间
- 订单状态
- 总金额
- 发货状态

---

## 6. 模块结构

### 6.1 总体结构
项目采用模块化单体架构，前后端分离开发，但部署优先保持简单。

```text
hill-commerce
├── common
├── framework
├── modules
│   ├── user
│   ├── product
│   ├── cart
│   ├── order
│   ├── payment
│   ├── shipping
│   ├── admin
│   ├── audit
│   └── notification
└── frontend
    └── next-app
```

### 6.2 模块职责

`common`
- 常量
- 枚举
- 通用异常
- 通用响应模型
- 工具类

`framework`
- Spring Boot 配置
- 安全配置
- Web 配置
- MyBatis-Plus 配置
- 会话与鉴权配置
- 邮件配置
- Redis 配置预留
- 领域事件发布器抽象预留

`user`
- 用户注册登录
- 角色管理
- 地址管理
- 用户资料

`product`
- 分类管理
- SPU 管理
- SKU 管理
- 销售属性
- 展示属性
- 商品图片
- 上下架 / 逻辑删除

`cart`
- 购物车
- 购物车项
- 勾选结算逻辑

`order`
- 创建订单
- 订单项快照
- 取消订单
- 确认收货
- 自动关闭
- 自动完成

`payment`
- 模拟支付
- 支付流水
- 真实支付接口预留

`shipping`
- 发货信息
- 运单录入
- 发货状态管理

`admin`
- 后台管理入口
- 角色权限管理

`audit`
- 登录日志
- 操作日志
- 商品浏览日志

`notification`
- 邮件通知
- 邮件日志

`framework/cache` 或等价基础设施层
- Redis 接口预留
- 缓存 Key 规范预留

`framework/events` 或等价基础设施层
- 领域事件模型预留
- 本地事件发布器预留
- RocketMQ 发布器扩展位预留

---

## 7. 数据库表设计

### 7.1 账号与权限

#### `users`
字段：
- id
- email
- password_hash
- nickname
- status
- last_login_at
- created_at
- updated_at

说明：
- 存放 Customer、Sales、Admin 全部账号
- `email` 唯一
- `status` 表示启用、禁用等状态

#### `roles`
字段：
- id
- code
- name
- created_at

建议值：
- `CUSTOMER`
- `SALES`
- `ADMIN`

#### `user_roles`
字段：
- id
- user_id
- role_id

关系：
- `users` 与 `roles` 多对多

### 7.2 地址

#### `user_addresses`
字段：
- id
- user_id
- receiver_name
- receiver_phone
- province
- city
- district
- detail_address
- postal_code
- is_default
- created_at
- updated_at

关系：
- 一个用户对应多个地址

### 7.3 分类与商品

#### `product_categories`
字段：
- id
- name
- sort_order
- status
- created_at
- updated_at

#### `products`
字段：
- id
- category_id
- name
- spu_code
- subtitle
- cover_image_url
- description
- min_sale_price
- status
- deleted
- deleted_at
- created_at
- updated_at

说明：
- `description` 为富文本内容
- `min_sale_price` 用于前台快速显示最低 SKU 价格
- 商品删除采用逻辑删除

#### `product_images`
字段：
- id
- product_id
- image_url
- sort_order
- created_at

说明：
- 存储封面图与详情图

### 7.4 销售属性、展示属性与 SKU

#### `product_sales_attributes`
字段：
- id
- product_id
- attribute_name
- sort_order

说明：
- 用于定义销售属性，如颜色、尺寸

#### `product_sales_attribute_values`
字段：
- id
- product_id
- sales_attribute_id
- attribute_value
- sort_order

#### `product_attribute_values`
字段：
- id
- product_id
- attribute_name
- attribute_value
- sort_order

说明：
- 展示属性，仅用于详情展示

#### `product_skus`
字段：
- id
- product_id
- sku_code
- sales_attr_value_key
- sales_attr_value_text
- price
- stock
- low_stock_threshold
- status
- deleted
- deleted_at
- created_at
- updated_at

说明：
- 每个 SKU 独立价格、库存、低库存阈值
- `sku_code` 自动生成，允许修改
- SKU 删除建议也支持逻辑删除

### 7.5 购物车

#### `carts`
字段：
- id
- user_id
- created_at
- updated_at

#### `cart_items`
字段：
- id
- cart_id
- product_id
- sku_id
- quantity
- selected
- created_at
- updated_at

说明：
- 购物车项以 `sku_id` 为准

### 7.6 订单与订单项

#### `orders`
字段：
- id
- order_no
- user_id
- order_status
- total_amount
- payable_amount
- payment_deadline_at
- paid_at
- shipped_at
- completed_at
- cancelled_at
- cancel_reason
- address_snapshot_name
- address_snapshot_phone
- address_snapshot_province
- address_snapshot_city
- address_snapshot_district
- address_snapshot_detail
- address_snapshot_postal_code
- created_at
- updated_at

说明：
- 保存地址快照
- `payment_deadline_at` 用于超时关闭

#### `order_items`
字段：
- id
- order_id
- product_id
- sku_id
- product_name_snapshot
- sku_code_snapshot
- sku_attr_text_snapshot
- product_image_snapshot
- unit_price
- quantity
- subtotal_amount
- created_at

说明：
- 必须保存商品和 SKU 快照

#### `order_status_histories`
字段：
- id
- order_id
- from_status
- to_status
- changed_by
- change_reason
- created_at

### 7.7 支付

#### `payments`
字段：
- id
- order_id
- payment_no
- payment_method
- payment_status
- amount
- requested_at
- paid_at
- fail_reason
- created_at
- updated_at

说明：
- MVP 中 `payment_method` 为模拟支付
- 结构预留真实支付扩展

### 7.8 物流

#### `shipments`
字段：
- id
- order_id
- carrier_name
- tracking_no
- shipment_status
- remark
- operated_by
- created_at
- updated_at

### 7.9 日志与审计

#### `login_logs`
字段：
- id
- user_id
- email_snapshot
- role_snapshot
- login_result
- ip_address
- user_agent
- login_at

#### `operation_logs`
字段：
- id
- operator_user_id
- operator_role
- action_type
- target_type
- target_id
- action_detail
- ip_address
- created_at

#### `product_view_logs`
字段：
- id
- user_id
- anonymous_id
- product_id
- category_id
- viewed_at

说明：
- 未登录浏览时使用 `anonymous_id`

### 7.10 邮件日志

#### `mail_logs`
字段：
- id
- biz_type
- biz_id
- recipient_email
- subject
- send_status
- sent_at
- fail_reason
- created_at

### 7.11 核心关系
- `users` -> `user_addresses`
- `users` -> `roles` 通过 `user_roles`
- `product_categories` -> `products`
- `products` -> `product_images`
- `products` -> `product_sales_attributes`
- `product_sales_attributes` -> `product_sales_attribute_values`
- `products` -> `product_attribute_values`
- `products` -> `product_skus`
- `users` -> `carts`
- `carts` -> `cart_items`
- `users` -> `orders`
- `orders` -> `order_items`
- `orders` -> `payments`
- `orders` -> `shipments`
- `orders` -> `order_status_histories`
- `users` -> `login_logs`
- `users` -> `operation_logs`
- `products` -> `product_view_logs`

### 7.12 字段约束说明

#### 通用字段约束
- 所有主键 `id` 使用单一主键策略，首版建议使用雪花 ID 或数据库自增 ID，项目内保持统一。
- 所有时间字段统一使用一个时区标准存储，建议数据库与应用统一使用 UTC，前端按用户时区展示。
- 所有金额字段使用定点数，禁止使用浮点类型。
- 所有布尔语义字段如 `deleted`、`selected`、`is_default` 明确取值范围，不允许 `null` 表示状态。
- 所有状态字段使用受控枚举，不允许自由文本。

#### `users`
- `email`：必填，唯一，长度建议不超过 128。
- `password_hash`：必填，仅存储哈希值，不存储明文。
- `status`：必填，枚举值建议为 `ACTIVE`、`DISABLED`。

#### `roles`
- `code`：必填，唯一，不允许修改历史语义。

#### `user_addresses`
- `receiver_name`：必填。
- `receiver_phone`：必填，需进行手机号格式校验。
- `province`、`city`、`district`、`detail_address`：必填。
- `is_default`：同一用户最多一条为默认地址。

#### `product_categories`
- `name`：必填，同一层级下唯一。
- `status`：必填，建议值为 `ENABLED`、`DISABLED`。

#### `products`
- `category_id`：必填。
- `name`：必填。
- `spu_code`：必填，唯一。
- `description`：富文本，允许为空但建议在上架前补齐。
- `min_sale_price`：非负，按 SKU 最低价同步维护。
- `status`：必填，建议值为 `DRAFT`、`ON_SALE`、`OFF_SALE`。
- `deleted`：逻辑删除标记，默认未删除。

#### `product_images`
- `image_url`：必填。
- `sort_order`：非负整数。
- 同一商品需支持一张封面图和多张详情图，展示顺序由 `sort_order` 控制。

#### `product_sales_attributes`
- `attribute_name`：必填。
- 同一商品最多 2 条销售属性。

#### `product_sales_attribute_values`
- `attribute_value`：必填。
- 同一销售属性下的值不可重复。

#### `product_attribute_values`
- `attribute_name`：必填。
- `attribute_value`：必填。

#### `product_skus`
- `product_id`：必填。
- `sku_code`：必填，唯一，系统自动生成后允许修改。
- `sales_attr_value_key`：必填，用于唯一标识 SKU 组合。
- 同一商品下 `sales_attr_value_key` 唯一。
- `price`：非负。
- `stock`：非负整数。
- `low_stock_threshold`：非负整数。
- `status`：必填，建议值为 `ENABLED`、`DISABLED`。
- `deleted`：逻辑删除标记，默认未删除。

#### `carts`
- `user_id`：唯一，一个用户一辆购物车。

#### `cart_items`
- `cart_id`、`product_id`、`sku_id`：必填。
- `quantity`：正整数。
- 同一购物车内，同一 `sku_id` 只允许存在一条记录。
- `selected`：必填。

#### `orders`
- `order_no`：必填，唯一。
- `user_id`：必填。
- `order_status`：必填。
- `total_amount`、`payable_amount`：非负。
- `payment_deadline_at`：创建订单时必填。
- `cancel_reason`：取消时可填。
- 地址快照字段：创建订单时必填，不得依赖后续地址表修改。

#### `order_items`
- `order_id`、`product_id`、`sku_id`：必填。
- `product_name_snapshot`、`sku_code_snapshot`、`sku_attr_text_snapshot`：必填。
- `unit_price`：非负。
- `quantity`：正整数。
- `subtotal_amount`：非负，且应等于 `unit_price * quantity`。

#### `order_status_histories`
- `from_status`、`to_status`：必填。
- 每次订单状态变化都必须写入一条记录。

#### `payments`
- `order_id`：必填。
- `payment_no`：必填，唯一。
- `payment_method`：首版固定 `MOCK`，但设计上保留可扩展枚举。
- `payment_status`：必填，建议值为 `PENDING`、`SUCCESS`、`FAILED`。
- `amount`：非负。

#### `shipments`
- `order_id`：必填，建议一个订单一条主发货记录。
- `carrier_name`：发货时必填。
- `tracking_no`：发货时必填。
- `shipment_status`：必填，建议值为 `PENDING`、`SHIPPED`、`DELIVERED`。
- `operated_by`：记录操作人用户 ID。

#### `login_logs`
- `login_result`：必填，建议值为 `SUCCESS`、`FAILED`。
- `ip_address`：必填。
- `login_at`：必填。

#### `operation_logs`
- `action_type`、`target_type`、`target_id`：必填。
- `action_detail`：必填，记录操作摘要。

#### `product_view_logs`
- `product_id`：必填。
- 登录浏览写 `user_id`，游客浏览写 `anonymous_id`。
- `viewed_at`：必填。

#### `mail_logs`
- `biz_type`：必填，建议值为 `ORDER_CREATED`、`PAYMENT_SUCCESS`、`ORDER_SHIPPED`。
- `recipient_email`：必填。
- `send_status`：必填，建议值为 `SUCCESS`、`FAILED`。

---

## 8. API 初步设计

以下为 API 分组与核心接口范围，具体路径可在开发阶段微调。

### 8.1 认证与用户

前台认证：
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

任务 3 的认证与会话实现约束：
- 后端是唯一认证事实源，前端不保存 token，不自行推导角色真相。
- 浏览器通过 Session Cookie 维持登录态；前端所有当前用户读取均通过后端 `GET /api/auth/me` 完成。
- Spring Security 采用基于 Session 的认证，不依赖 Redis 才能完成登录、鉴权和注销闭环。
- 管理后台的角色边界至少支持 `ADMIN` 与 `SALES`；前台注册用户默认授予 `CUSTOMER`。
- 登录成功与失败都必须写入应用日志，禁止记录明文密码。

前端认证页面与受保护路由约束：
- 必须提供前端登录页 `GET /login` 与注册页 `GET /register`。
- 必须提供至少一个前台受保护页面用于验证未登录拦截，任务 3 使用 `/account` 作为最小闭环页面。
- 必须提供至少一个后台受保护页面用于验证角色边界，任务 3 使用 `/admin` 作为最小闭环页面。
- 必须提供无权限提示页 `/forbidden`，用于承接已登录但角色不满足的场景。

前端路由守卫与服务端鉴权策略：
- Next.js `middleware.ts` 仅做快速拦截，不在 middleware 内请求后端。
- 前端浏览器不直接向后端认证接口发起跨域请求，而是通过 Next.js Route Handlers 代理登录、注册、注销与 `me` 请求，使 Session Cookie 以同源方式存储在前端站点下。
- middleware 根据 Session Cookie 是否存在决定是否允许进入受保护前台页与后台页。
- Server Components 侧通过服务端辅助函数读取请求 Cookie，并转发到后端 `GET /api/auth/me` 获取当前用户。
- 前台受保护页面在服务端执行 `requireUser()`；后台受保护页面在服务端执行 `requireRole(["ADMIN", "SALES"])`。
- 已登录用户访问 `/login` 或 `/register` 时，应被重定向离开认证页，避免重复登录或注册。

日志约束：
- 登录成功日志至少包含 `email`、`userId`、`roles`、`remoteAddr`。
- 登录失败日志至少包含 `email`、`remoteAddr`、失败原因摘要。
- 日志先落应用日志，不要求在任务 3 新建数据库表。

任务 3 的验收映射：
- Customer 可注册、登录、注销，并可访问 `/account`。
- Sales/Admin 可登录后台并访问 `/admin`。
- 未登录访问 `/account` 与 `/admin` 时必须跳转到 `/login`。
- Customer 登录后访问 `/admin` 时必须进入 `/forbidden` 或等价无权限页面。
- 登录成功与失败都必须能在应用日志中看到记录。

用户地址：
- `GET /api/user/addresses`
- `POST /api/user/addresses`
- `PUT /api/user/addresses/{id}`
- `DELETE /api/user/addresses/{id}`
- `PUT /api/user/addresses/{id}/default`

### 8.2 前台商品

分类与商品浏览：
- `GET /api/categories`
- `GET /api/products`
- `GET /api/products/{productId}`
- `GET /api/products/{productId}/skus`

### 8.3 购物车
- `GET /api/cart`
- `POST /api/cart/items`
- `PUT /api/cart/items/{itemId}`
- `DELETE /api/cart/items/{itemId}`
- `PUT /api/cart/items/{itemId}/selected`

### 8.4 订单
- `POST /api/orders/preview`
- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderId}`
- `POST /api/orders/{orderId}/cancel`
- `POST /api/orders/{orderId}/confirm-receipt`

### 8.5 支付
- `POST /api/payments/orders/{orderId}/mock-pay`
- `GET /api/payments/orders/{orderId}`

### 8.6 后台商品管理
- `GET /api/admin/categories`
- `POST /api/admin/categories`
- `PUT /api/admin/categories/{id}`
- `DELETE /api/admin/categories/{id}`
- `GET /api/admin/products`
- `POST /api/admin/products`
- `GET /api/admin/products/{id}`
- `PUT /api/admin/products/{id}`
- `DELETE /api/admin/products/{id}`
- `PUT /api/admin/products/{id}/status`
- `GET /api/admin/products/{id}/skus`
- `POST /api/admin/products/{id}/skus`
- `PUT /api/admin/skus/{skuId}`
- `PUT /api/admin/skus/{skuId}/status`

任务 4 的后台商品管理实现约束：
- 只支持一级分类；分类字段至少包含 `name`、`sortOrder`、`status`。
- 分类状态仅允许 `ENABLED` 与 `DISABLED`。
- 商品管理采用 SPU 聚合保存模型；商品编辑页一次提交基础信息、图片、展示属性、销售属性与 SKU。
- 图片能力先采用 URL 录入，不实现上传系统。
- 商品描述先保存为富文本源字符串或 HTML 源字符串，不引入编辑器依赖。
- 销售属性最多允许 2 组；每组内属性值不可重复。
- SKU 必须从销售属性组合派生，组合键必须唯一。
- SKU 编码允许手工填写；若为空则由后端自动生成。
- 商品删除采用逻辑删除，不做物理删除。

分类管理规则：
- 分类名称必填且唯一。
- 分类排序必须为非负整数。
- 分类列表默认按 `sortOrder` 升序、`id` 升序展示。
- 禁用分类后不可用于新建商品，但历史商品详情仍可查看。

商品与 SKU 管理规则：
- 商品基础信息至少包括 `categoryId`、`name`、`spuCode`、`subtitle`、`coverImageUrl`、`description`、`status`。
- 商品状态至少支持 `DRAFT`、`ON_SHELF`、`OFF_SHELF`。
- `spuCode` 必填且唯一。
- 展示属性采用键值对列表维护。
- 详情图采用 URL 列表维护，并保留排序字段。
- SKU 至少包含 `skuCode`、`price`、`stock`、`lowStockThreshold`、`status`。
- `price`、`stock`、`lowStockThreshold` 必须是非负值。
- 上架商品前至少必须拥有 1 个 SKU。
- 逻辑删除商品后，后台默认列表不展示该商品。

SKU 自动编码规则：
- 若请求中的 SKU 未提供 `skuCode`，后端按 `SPU_CODE-001`、`SPU_CODE-002` 之类的顺序编码自动补全。
- 若请求中的 SKU 提供了 `skuCode`，后端只做唯一性校验，不改写用户输入。
- 编辑商品时，已有 SKU 保留原有编码；仅对新增且未填写编码的 SKU 自动补码。

后台页面结构约束：
- 必须提供 `/admin/categories` 分类管理页。
- 必须提供 `/admin/products` 商品列表页。
- 必须提供 `/admin/products/new` 商品新建页。
- 必须提供 `/admin/products/[id]` 商品编辑页。
- 商品编辑页按“基础信息 / 图片与描述 / 展示属性与销售属性 / SKU 列表”四个分区组织，但技术上仍为一次提交。

任务 4 的验收映射：
- Sales 可创建、编辑、停用、删除一级分类。
- Sales 可创建商品并维护展示属性、销售属性、SKU、图片 URL 与描述。
- 每个商品最多配置 2 个销售属性，并能生成唯一 SKU 组合。
- 商品可执行上架、下架、逻辑删除。
- SKU 的价格、库存、低库存阈值可独立维护。
- Customer 不可访问后台商品管理接口与页面。

### 8.7 后台订单与发货
- `GET /api/admin/orders`
- `GET /api/admin/orders/{id}`
- `POST /api/admin/orders/{id}/ship`
- `GET /api/admin/orders/{id}/shipment`

### 8.8 后台账号与权限
- `GET /api/admin/users`
- `POST /api/admin/users/{id}/roles`
- `PUT /api/admin/users/{id}/roles`
- `POST /api/admin/users/{id}/reset-password`

### 8.9 日志与统计
- `GET /api/admin/logins`
- `GET /api/admin/operations`
- `GET /api/admin/product-views`
- `GET /api/admin/dashboard/summary`

### 8.10 定时任务相关业务
这些一般不对外暴露 API，但必须在系统中实现：
- 关闭超时未支付订单
- 回补超时取消订单库存
- 自动完成已发货 10 天未确认收货订单
- 发送业务邮件

### 8.11 事件与缓存扩展边界
MVP 首版不要求对外提供 Redis 或 RocketMQ 管理接口，但在应用内部应预留以下抽象：

建议缓存抽象：
- 商品详情缓存读取/失效
- 热门商品缓存读取/刷新

建议事件抽象：
- `OrderCreated`
- `OrderCancelled`
- `PaymentSucceeded`
- `OrderShipped`
- `OrderCompleted`

实现要求：
- 首版可先采用本地同步实现
- 后续替换为 Redis 缓存和 RocketMQ 消息投递时，不应破坏业务层接口

---

## 9. 数据采集方案

### 9.1 MVP 必采集数据

登录日志：
- 账号
- 角色
- 登录时间
- IP 地址
- User-Agent
- 登录结果

商品浏览日志：
- 用户 ID 或匿名标识
- 商品 ID
- 分类 ID
- 浏览时间

后台操作日志：
- 操作账号
- 角色
- 操作时间
- IP 地址
- 操作类型
- 操作对象
- 操作内容摘要

订单与支付数据：
- 订单号
- 用户
- 商品 / SKU
- 单价
- 数量
- 总价
- 下单时间
- 支付时间
- 订单状态
- 支付状态

发货数据：
- 订单号
- 发货时间
- 快递公司
- 运单号
- 操作人

### 9.2 采集方式建议
- 登录日志：在认证成功/失败时记录
- 操作日志：在后台关键写操作后记录
- 浏览日志：在商品详情访问时记录
- 订单数据：在下单、支付、取消、发货、收货时记录状态变化
- 邮件日志：在通知发送完成后记录

### 9.3 MVP 不采集或不精确采集的数据
- 精确停留时长
- 页面完整访问路径
- 推荐曝光 / 点击 / 转化链路
- 用户画像标签
- 设备渠道细分
- 实时流式行为事件

---

## 10. 推荐系统 MVP 方案

### 10.1 目标
首版不做正式推荐系统，但要保留最小可扩展方案，用于后续接入推荐逻辑。

### 10.2 MVP 推荐策略
前台可预留一个简单推荐区块，采用以下低成本规则之一：

- 同分类商品推荐
- 热门商品推荐
- 最近浏览商品推荐

### 10.3 推荐实现建议
MVP 推荐不做算法训练，不做协同过滤正式实现。建议采用规则型方案：

- 商品详情页底部展示“同分类推荐”
- 首页展示“热门商品”或“最新上架”
- 登录用户可在个人中心展示“最近浏览”

### 10.4 推荐所依赖的最小数据基础
- 商品分类
- 商品上架状态
- 商品浏览日志
- 订单销量统计

### 10.5 后续扩展方向
后续版本可逐步升级为：
- “看过该商品的人也买了”
- 基于浏览和购买记录的协同过滤
- 个性化用户画像推荐
- 热度与转化率结合的排序策略
- 基于 Redis 热点数据和 RocketMQ 异步事件的推荐特征沉淀

---

## 11. 开发任务拆分

### 任务 1：项目骨架与基础环境
内容：
- 初始化后端工程结构
- 初始化前端工程结构
- 配置 MySQL、Flyway、基础环境
- 建立模块化单体目录

交付结果：
- 前后端可启动
- 数据库可连接
- 基础迁移脚本机制可运行

### 任务 2：统一认证与角色权限
内容：
- 注册、登录、注销
- Session/Cookie
- 角色模型
- 后台访问控制

交付结果：
- Customer、Sales、Admin 基础认证可用
- 前后台鉴权规则生效

### 任务 3：商品分类、SPU、SKU 后台管理
内容：
- 一级分类
- 商品 SPU
- SKU
- 销售属性
- 展示属性
- 富文本描述
- 商品图片管理

交付结果：
- 后台可以完整维护可售商品

### 任务 4：前台商品浏览与搜索
内容：
- 分类列表
- 商品列表
- 商品详情
- 商品名称搜索

交付结果：
- 访客可完成浏览和搜索

### 任务 5：购物车与地址管理
内容：
- 加购
- 修改购物车
- 地址管理
- 默认地址

交付结果：
- 登录用户可完成购物前准备

### 任务 6：下单、库存扣减、订单快照
内容：
- 结算校验
- 创建订单
- 库存扣减
- 地址快照
- 订单项快照

交付结果：
- 正确生成订单并扣减库存

### 任务 7：模拟支付与订单超时关闭
内容：
- 模拟支付
- 支付流水
- 手动取消未支付订单
- 30 分钟自动关闭并回补库存

交付结果：
- 支付和取消链路完整

### 任务 8：发货与收货闭环
内容：
- 发货录入
- 物流展示
- 确认收货
- 发货 10 天自动完成

交付结果：
- 订单履约链路完整

### 任务 9：日志、邮件与基础统计
内容：
- 登录日志
- 操作日志
- 浏览日志
- 邮件通知
- Dashboard 基础统计

交付结果：
- 审计与统计能力可用

### 任务 10：联调与上线准备
内容：
- 前后端联调
- 异常处理
- 权限补充校验
- 部署与演示链路验证

交付结果：
- 系统可稳定演示和部署

---

## 12. 验收标准

### 12.1 功能验收标准

用户侧：
- 用户可通过邮箱注册并登录
- 未登录用户可浏览商品，但不可加购和下单
- 登录用户可加入购物车、管理地址、提交订单
- 订单提交后库存立即扣减
- 用户可完成模拟支付
- 未支付订单可取消
- 用户可查看订单和物流信息
- 用户可确认收货

后台侧：
- Sales 可维护分类、商品、SKU、库存和上下架状态
- Sales 可查看全部订单并录入发货信息
- Admin 可管理角色与密码重置
- Admin 可查看日志和基础统计

状态流转：
- 订单创建后进入待支付
- 超时未支付会自动取消并回补库存
- 支付成功后进入已支付
- 发货后进入已发货
- 确认收货后完成
- 发货 10 天后可自动完成

数据与审计：
- 商品、SKU、订单、支付、物流数据存储正确
- 关键状态变化有历史记录
- 登录日志、操作日志、浏览日志可查询
- 邮件发送结果有日志

### 12.2 非功能验收标准
- 系统可本地稳定启动
- 数据库变更可通过迁移执行
- 前后台权限隔离有效
- 关键异常场景有明确提示
- 主流程不依赖人工改库或手工修数
- 可完成完整演示链路：
  - 浏览商品
  - 注册登录
  - 加入购物车
  - 提交订单
  - 模拟支付
  - 后台发货
  - 用户确认收货

### 12.3 MVP 完成判定
当以下条件同时满足时，可认定 MVP 完成：

- 交易闭环完整可跑通
- 后台商品与订单管理闭环完整可跑通
- 审计、日志、统计具备基础可用性
- 技术结构支持后续接真实支付、推荐和更多商品形态扩展

---

## 附录 A：状态流转表

### A.1 订单状态流转表

| 当前状态 | 触发条件 | 下一个状态 | 触发方 | 备注 |
| --- | --- | --- | --- | --- |
| 无 | 用户提交订单成功 | `PENDING_PAYMENT` | 系统 | 同时创建订单、订单项并扣减 SKU 库存 |
| `PENDING_PAYMENT` | 用户主动取消未支付订单 | `CANCELLED` | Customer | 需要回补库存 |
| `PENDING_PAYMENT` | 订单创建后 30 分钟仍未支付 | `CANCELLED` | 系统定时任务 | 需要回补库存 |
| `PENDING_PAYMENT` | 模拟支付成功 | `PAID` | Customer / 系统 | 写入支付记录 |
| `PAID` | Sales 录入快递公司和运单号并发货 | `SHIPPED` | Sales | 同时写入发货记录 |
| `SHIPPED` | 用户确认收货 | `COMPLETED` | Customer | 写入状态变更历史 |
| `SHIPPED` | 发货后满 10 天未确认收货 | `COMPLETED` | 系统定时任务 | 自动完成 |

约束：
- `CANCELLED` 为终态，不允许再支付或发货。
- `COMPLETED` 为终态。
- 只有 `PENDING_PAYMENT` 状态允许取消。
- 只有 `PAID` 状态允许发货。
- 只有 `SHIPPED` 状态允许确认收货或自动完成。

### A.2 支付状态流转表

| 当前状态 | 触发条件 | 下一个状态 | 触发方 | 备注 |
| --- | --- | --- | --- | --- |
| 无 | 创建支付请求 | `PENDING` | 系统 | 生成支付流水 |
| `PENDING` | 模拟支付成功 | `SUCCESS` | Customer / 系统 | 同步推动订单进入 `PAID` |
| `PENDING` | 模拟支付失败 | `FAILED` | Customer / 系统 | 保留失败原因 |

约束：
- 同一支付流水不允许从 `SUCCESS` 回退。
- 订单首版按单次成功支付闭环处理，不做拆单支付和部分支付。

### A.3 发货状态流转表

| 当前状态 | 触发条件 | 下一个状态 | 触发方 | 备注 |
| --- | --- | --- | --- | --- |
| `PENDING` | Sales 完成发货录入 | `SHIPPED` | Sales | 录入快递公司和运单号 |
| `SHIPPED` | 用户确认收货或订单自动完成 | `DELIVERED` | Customer / 系统 | 用于物流记录层状态 |

约束：
- 发货记录层状态服务于物流展示。
- 订单主流程仍以 `orders.order_status` 为准。

### A.4 商品状态流转表

| 当前状态 | 触发条件 | 下一个状态 | 触发方 | 备注 |
| --- | --- | --- | --- | --- |
| `DRAFT` | Sales 上架商品 | `ON_SALE` | Sales | 必须存在可售 SKU |
| `ON_SALE` | Sales 下架商品 | `OFF_SALE` | Sales | 前台不可购买 |
| `OFF_SALE` | Sales 重新上架 | `ON_SALE` | Sales | 需满足可售条件 |

约束：
- 逻辑删除与商品状态分离，删除不等于下架。
- 上架前必须满足基础信息完整、至少存在一个启用且库存有效的 SKU。

### A.5 SKU 状态流转表

| 当前状态 | 触发条件 | 下一个状态 | 触发方 | 备注 |
| --- | --- | --- | --- | --- |
| `ENABLED` | Sales 禁用 SKU | `DISABLED` | Sales | 不可继续加入购物车或结算 |
| `DISABLED` | Sales 启用 SKU | `ENABLED` | Sales | 恢复可售 |

约束：
- `DISABLED` 的 SKU 不允许参与新订单创建。
- 已存在于购物车中的禁用 SKU，在结算时必须被再次校验并阻止提交。
