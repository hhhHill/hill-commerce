# Tasks: hill-commerce-mvp

**Feature**: `hill-commerce-mvp`  
**Status**: active  
**Source**: Derived and normalized from `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md`

## Purpose

本文件是 `hill-commerce-mvp` 的当前执行视图，用于指导后续逐步推进 MVP，而不是一次性展开所有实现细节。

它的目标是：

- 给个人开发者或 AI agent 一个可顺序执行的 MVP 路线图
- 明确每一阶段的目标、范围、交付物与完成标志
- 在不拆分 `plan.md` 的前提下，保持执行层足够清晰
- 为后续在复杂模块上拆出独立 feature spec 留出边界

本文件不负责：

- 代替 `spec.md` 定义产品事实
- 代替 `plan.md` 定义技术架构
- 细化到单次提交级别的实施步骤

## Usage Rules

- 开始任何中高复杂度实现前，先读 `.specify/memory/constitution.md`
- 再读 `specs/hill-commerce-mvp/spec.md`
- 再读 `specs/hill-commerce-mvp/plan.md`
- 最后根据本文件选择当前要推进的阶段与任务

如果某个主题在推进过程中变复杂：

- 先停止继续在 baseline 内细化
- 判断是否需要拆成独立 `specs/<feature-name>/`
- 高风险主题优先拆分，不强行塞回 baseline

当前第一轮 phase-based feature 分流为：

- `auth-permission`
- `admin-product-management`
- `shopping-journey`
- `order-checkout`
- `payment`
- `fulfillment`
- `operations-observability`

## Execution Strategy

当前 MVP 推荐按以下顺序推进：

1. 先完成“基础骨架 + 认证 + 后台商品管理”闭环
2. 再完成“前台浏览 + 购物车 + 下单 + 支付”闭环
3. 最后完成“履约 + 审计 + 统计 + 联调上线准备”闭环

这样做的原因是：

- 当前仓库已经具备后端骨架、Flyway 基线、认证和后台商品管理的部分实现
- 商品与权限基础不稳定时，继续推进购物车、订单、支付会导致返工
- 订单、支付、库存、发货是高风险模块，应在前置基础稳定后再继续推进

## Phase 0: 规范与迁移基线

### Goal

确保后续所有实现都以 canonical spec 为准，而不是继续回到旧 `docs/superpowers/specs/*` 与 `docs/superpowers/plans/*`。

### Scope

- constitution
- baseline spec / plan / tasks
- superpowers 历史文档废弃标记
- agent 入口指向

### Deliverables

- `.specify/memory/constitution.md`
- `specs/README.md`
- `specs/migration-map.md`
- `specs/hill-commerce-mvp/spec.md`
- `specs/hill-commerce-mvp/plan.md`
- `specs/hill-commerce-mvp/tasks.md`
- 旧 superpowers spec / plan 的 deprecated notice

### Completion Signal

- 后续任务默认引用 `specs/*`
- `AGENTS.md` 已指向 canonical baseline
- 不再把旧 superpowers spec / plan 当成 active source

## Phase 1: 基础骨架与权限闭环

### Goal

建立一个可持续扩展的工程底座，并完成最小可用的认证和后台访问边界。

### Included Tasks

- [ ] 任务 1：工程骨架与基础设施
- [ ] 任务 2：数据库基线与迁移体系
- [ ] 任务 3：认证、会话与权限体系

### Scope Notes

- 保持后端为模块化单体
- 保持前端通过 Next.js 同源代理认证请求
- 保持 Redis / RocketMQ 为预留能力，而不是运行前提
- 所有实现应在现有目录骨架上增量扩展，不重建工程

### Deliverables

- 可启动的后端与前端骨架
- Flyway 基线与角色种子
- 可用的注册、登录、注销和角色校验能力
- `/account` 与 `/admin` 的最小受保护路径

### Completion Signal

- Customer 可注册、登录、注销并访问前台受保护页
- Sales / Admin 可访问后台
- 未登录和无权限跳转链路正确
- 数据库基线可重复执行

### Dependency

- 这是所有后续业务任务的共同前置

### Split Hint

若认证、角色权限、路由保护继续扩展到多页面、多角色、多策略，优先拆出 `auth-permission`。

## Phase 2: 后台商品管理闭环

### Goal

完成后台商品可维护能力，让后续购物车、下单、支付依赖的数据基础稳定可用。

### Included Tasks

- [ ] 任务 4：后台商品管理

### Scope Notes

- 只支持一级分类
- 商品采用 `SPU + SKU`
- 销售属性最多 2 组
- 商品管理保持一次提交的聚合保存模型
- 图片继续采用 URL 录入
- 逻辑删除与商品状态分离

### Deliverables

- 分类管理
- 商品新增 / 编辑
- SKU 自动补码与手工编码
- 商品上架 / 下架 / 逻辑删除

### Completion Signal

- Sales 可维护可售商品数据
- 前台浏览所需的最小商品数据完整
- 商品状态与 SKU 状态边界清晰

### Dependency

- 依赖 Phase 1 的后台认证与权限边界

### Split Hint

若商品后台继续扩展到更复杂的属性体系、审核流、图片上传或批量操作，优先拆出 `admin-product-management`。

## Phase 3: 前台浏览闭环

### Goal

完成访客可浏览、可搜索、可查看详情的前台入口，为购物链路提供真实使用场景。

### Included Tasks

- [ ] 任务 5：前台商品浏览与搜索

### Scope Notes

- 访客可浏览商品
- 前台支持按商品名称搜索
- 商品详情需体现最小展示规则
- 商品浏览日志属于本阶段的延伸要求

### Deliverables

- 首页最小商品展示
- 分类或商品列表页
- 商品详情页
- 搜索能力

### Completion Signal

- 未登录用户可完成浏览与搜索
- 商品详情信息足以支撑后续加购与下单

### Dependency

- 依赖 Phase 2 的商品数据基础

## Phase 4: 购物准备闭环

### Goal

完成购物车与地址管理，为下单做准备。

### Included Tasks

- [ ] 任务 6：购物车与地址管理

### Scope Notes

- 购物车只支持登录用户
- 不支持匿名购物车
- 不支持登录后购物车合并
- 地址支持默认地址
- 购物车需识别失效 SKU / 库存不足等异常情况

### Deliverables

- 购物车增删改查
- 地址增删改查与默认地址
- 结算前勾选与汇总视图

### Completion Signal

- 用户可完成加购、修改数量、管理地址
- 结算前可识别失效商品或 SKU

### Dependency

- 依赖 Phase 1 的认证闭环
- 依赖 Phase 3 的前台浏览闭环

### Split Hint

若购物车规则、价格展示、勾选结算、库存预校验显著变复杂，优先拆出 `cart`。

## Phase 5: 下单与库存闭环

### Goal

完成从结算到订单创建的核心交易闭环，并保证订单快照与库存一致性。

### Included Tasks

- [ ] 任务 7：下单、库存扣减与订单快照

### Scope Notes

- 下单前再次校验商品、SKU、库存、地址
- 订单创建后立即扣减库存
- 必须保存商品快照与地址快照
- 用户可主动取消未支付订单并触发库存回补

### Deliverables

- 结算预览
- 订单创建
- 订单项快照
- 订单状态历史
- 库存扣减与取消回补

### Completion Signal

- 订单可稳定创建
- 快照数据正确
- 取消规则与库存回补规则正确

### Dependency

- 依赖 Phase 2 和 Phase 4

### Split Hint

本阶段一旦出现复杂状态机、库存预占、并发控制、订单拆分等需求，应优先拆出：

- `order`
- `inventory`
- `checkout`

## Phase 6: 支付闭环

### Goal

在不接入真实支付渠道的前提下，完成模拟支付与超时关闭闭环。

### Included Tasks

- [ ] 任务 8：模拟支付与超时关闭

### Scope Notes

- 首版只支持模拟支付
- 要有支付流水
- 要处理超时关闭与库存回补
- 要考虑重复支付、重复关闭等幂等问题

### Deliverables

- 模拟支付接口
- 支付成功状态流转
- 30 分钟超时关闭任务
- 库存回补与支付幂等约束

### Completion Signal

- 用户可完成模拟支付
- 超时未支付订单可被自动关闭
- 支付和关闭路径都有基本幂等保护

### Dependency

- 依赖 Phase 5

### Split Hint

本阶段默认是最高优先级拆分候选之一；若继续细化，应优先拆出 `payment`。

## Phase 7: 履约闭环

### Goal

完成从已支付到发货、查单、确认收货、自动完成的履约链路。

### Included Tasks

- [ ] 任务 9：发货、物流、确认收货与自动完成

### Scope Notes

- 后台 Sales 录入快递公司与运单号
- 前台用户查看订单与物流
- 用户确认收货
- 发货后 10 天可自动完成

### Deliverables

- 后台订单与发货页
- 前台订单列表与详情页
- 物流展示
- 自动完成任务

### Completion Signal

- 已支付订单可发货
- 用户可查单、查物流、确认收货
- 自动完成规则可执行

### Dependency

- 依赖 Phase 6

### Split Hint

若发货、物流状态、履约 SLA、售后前置流程复杂化，应从 baseline 中拆出 `order` 或 `refund-after-sales` 的后续规范。

## Phase 8: 审计、通知与统计闭环

### Goal

补齐最小可用的运维、审计和管理视角。

### Included Tasks

- [ ] 任务 10：日志、邮件、基础统计与后台审计

### Scope Notes

- 登录日志、操作日志、浏览日志属于 MVP 范围
- 邮件失败不应阻断主交易流程
- Dashboard 只做基础统计，不做高级分析大屏

### Deliverables

- 日志查询页
- 邮件通知与发送记录
- Dashboard 汇总视图

### Completion Signal

- 登录、操作、浏览日志可查询
- 下单 / 支付 / 发货相关邮件链路可追踪
- Dashboard 具备基础业务汇总能力

### Dependency

- 可与 Phase 5、6、7 穿插推进，但要建立在业务事件具备可记录点的前提上

## Phase 9: 联调与上线准备

### Goal

把前面各阶段能力组合成完整可演示链路，并补齐最小异常体验与部署说明。

### Included Tasks

- [ ] 任务 11：联调、异常处理与上线准备

### Scope Notes

- 这不是新业务功能阶段
- 重点是接口联调、异常路径、演示走查、部署验证
- 不在这个阶段引入新的产品需求

### Deliverables

- 联调后的主流程
- `403 / 404 / 500` 页面
- 部署与演示说明

### Completion Signal

- 可完整演示“浏览 -> 注册登录 -> 加购 -> 下单 -> 支付 -> 发货 -> 收货”
- 未启用 Redis / RocketMQ 时主流程仍可运行
- 常见异常路径具备清晰提示

### Dependency

- 依赖前面所有核心业务阶段

## Current Split Candidates

以下主题已被选定为第一轮 phase-based feature 分流目标：

- [ ] `auth-permission`
- [ ] `admin-product-management`
- [ ] `shopping-journey`
- [ ] `order-checkout`
- [ ] `payment`
- [ ] `fulfillment`
- [ ] `operations-observability`

## Future Split Candidates

以下主题保留为后续按复杂度继续细拆的候选：

- [ ] `inventory`
- [ ] `promotion`
- [ ] `refund-after-sales`

## Cross-Phase Dependencies

- Phase 1 是所有后续任务前置
- Phase 2 依赖 Phase 1
- Phase 3 依赖 Phase 2 的商品可用数据
- Phase 4 依赖 Phase 1 与 Phase 3
- Phase 5 依赖 Phase 2 与 Phase 4
- Phase 6 依赖 Phase 5
- Phase 7 依赖 Phase 6
- Phase 8 可穿插推进，但依赖前序业务事件逐步就绪
- Phase 9 最后进行

## Ready-to-Implement Order

如果后续直接进入实现，推荐默认执行顺序为：

1. Phase 1
2. Phase 2
3. Phase 3
4. Phase 4
5. Phase 5
6. Phase 6
7. Phase 7
8. Phase 8
9. Phase 9

## Extraction Rule

若某一任务继续细化时出现以下情况，应停止继续在 baseline 内扩写，并先拆独立 feature：

- 出现独立状态机
- 涉及金额、库存、权限、外部回调
- 出现独立 API 组和独立数据模型
- 子任务超过 5 个且能独立执行
- 会被单独长期迭代
- 规则复杂到无法在 baseline 中清晰表达

## Notes

- 本文件保留“阶段级 + 任务级”粒度，适合后续逐步执行
- 当某个阶段真正进入实施前，可在对应独立 feature 中再细化为更小的 implementation tasks
