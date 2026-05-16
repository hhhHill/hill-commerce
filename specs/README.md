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
2. 目标 `specs/<feature>/spec.md`
3. 目标 `specs/<feature>/plan.md`
4. 目标 `specs/<feature>/tasks.md`
5. 本文件
6. `specs/migration-map.md`

如果与 `specs/<feature>/spec.md` 冲突，以具体 feature spec 为准。

## Current Baseline

当前 canonical baseline 为：


这个 baseline 承载当前商城 MVP 的项目目标、范围、角色、全局非目标、核心模块总览和跨模块约束。

当前第一阶段只要求存在这一组 baseline 文档，不要求预先创建未来按需 feature 目录。

## Naming Rules

- 只有当某个模块、子系统或功能复杂到需要独立规范生命周期时，才创建新的 `specs/<feature-name>/`
- feature 名称使用短横线命名，如 `order`、`payment`、`admin-product-management`
- 每个 feature 目录内固定三件套：
- `spec.md`
- `plan.md`
- `tasks.md`
- 目标目录树在第一阶段只展示必备目录，不展示未来按需 feature 目录

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
