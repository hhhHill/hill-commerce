# Specs Guide

## Purpose

`specs/*` 是本仓库的 canonical specification layer。

它负责承载：

- 业务边界
- 产品规则
- 技术执行方案
- 当前任务拆解

它不负责承载：

- AI prompt 本身
- 执行工作流模板
- 历史 superpowers 资产
- 本地启动手册和安装说明

## Reading Order

开始任何中高复杂度开发前，按以下顺序阅读：

1. `.specify/memory/constitution.md`
2. 目标 `specs/<module>/<feature>/spec.md`
3. 目标 `specs/<module>/<feature>/plan.md`
4. 目标 `specs/<module>/<feature>/tasks.md`
5. 本文件
6. `specs/migration-map.md`

如果与具体 feature spec 冲突，以具体 feature spec 为准。

## Current Baseline

当前 canonical baseline 为：


这个 baseline 承载当前商城 MVP 的项目目标、范围、角色、全局非目标、核心模块总览和跨模块约束。

当前第一阶段只要求存在这一组 baseline 文档，不要求预先创建未来按需 feature 目录。

## Directory Structure

```
specs/
├── README.md
├── admin/                       # 管理后台模块
│   ├── account-management/      # 账户管理与仪表盘
│   ├── analytics/               # 数据分析
│   └── product-management/      # 后台商品管理
├── order/                       # 订单模块
│   ├── center/                  # 订单中心
│   ├── checkout/                # 订单结算
│   └── fulfillment/             # 履约发货
├── product/                     # 商品模块
│   └── discovery/               # 商品发现与浏览
├── cart/                        # 购物车模块
├── payment/                     # 支付模块
├── oss/                         # OSS 图片上传
├── recommendation/              # 推荐引擎
├── platform/                    # 平台级（跨模块）
│   ├── auth-permission/         # 认证与权限
│   ├── merchant-platform/       # 多商家改造
│   ├── scheduler-separation/    # 调度器拆分
│   └── observability/           # 运维可观测性
└── storefront/                  # 前台体验（前端为主）
    └── ux-transformation/       # UX 改造
```

## Naming Rules

- 第一级目录按模块分组，对应 backend `modules/` 或前端 `features/`，跨模块的放 `platform/`，前端为主的放 `storefront/`
- 第二级为具体 feature，路径为 `specs/<module>/<feature>/`
- 当模块只有一个 feature 时，可直接放在模块目录下（如 `specs/cart/spec.md`）
- feature 名称使用短横线命名，如 `account-management`、`product-management`
- 每个 feature 目录内固定三件套：
  - `spec.md` — 业务事实与验收意图
  - `plan.md` — 技术方案、边界和关键实现约束
  - `tasks.md` — 当前可执行任务视图

## Update Rules

- `spec.md` 定义业务事实与验收意图
- `plan.md` 定义技术方案、边界和关键实现约束
- `tasks.md` 定义当前可执行任务视图
- `README.md` 只定义规范层入口、命名规则和阅读顺序
- `migration-map.md` 只记录迁移映射、来源和 `needs-decision`

## Lifecycle Visibility

- `active` spec 可以定义当前产品行为
- `deprecated` spec 仅保留迁移参考，不应继续新增行为规则
- `superseded` spec 必须明确指向 replacement spec
- `archived` spec 仅保留历史价值，不再定义当前行为
- lifecycle 状态应在 spec 文件头部或对应 README 中可见
- 若 lifecycle 记录与具体 active spec 冲突，以 active spec 为准

## Split Triggers

当某个主题满足以下任一条件时，应考虑从 baseline 拆出独立 feature spec：

- 有独立状态机
- 涉及金额、库存、权限、外部回调或其他高风险业务
- 有独立 API 分组
- 有独立数据模型或数据库表
- 需要 5 个以上可独立执行的 implementation tasks
- 会被单独持续迭代
- 规则复杂到无法在 baseline 中清晰表达
