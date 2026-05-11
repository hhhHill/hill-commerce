<!--
Sync Impact Report
- Version change: 1.0.0 -> 1.1.0
- Modified principles:
  - V. Scaled Workflow -> V. Scaled Spec Governance
- Added sections:
  - V. Spec Lifecycle
- Removed sections:
  - Workflow Rules
- Templates requiring updates:
  - ✅ .specify/templates/plan-template.md
  - ✅ .specify/templates/spec-template.md
  - ✅ .specify/templates/tasks-template.md
  - ✅ AGENTS.md
  - ✅ specs/README.md
- Follow-up TODOs:
  - None
-->
# hill-commerce Constitution

## Core Principles

### I. Canonical Spec First
所有规范性的产品行为、业务规则、架构边界与验收标准，必须以 `specs/*` 为准。

适用顺序：

1. 本文件 `.specify/memory/constitution.md`
2. `specs/<feature>/spec.md`
3. `specs/<feature>/plan.md`
4. `specs/<feature>/tasks.md`
5. `specs/README.md`
6. `specs/migration-map.md`

`docs/superpowers/` 下的 prompts、workflows、commands、checklists 属于执行辅助文档，不定义产品事实。

改变产品行为、架构边界、数据契约或高风险业务逻辑的工作，必须能够追溯到 canonical specification system。

### II. No Dual Truth
不得在 `specs/*` 之外维护另一套长期有效的产品或架构真相。

约束：

- `docs/superpowers/specs/*` 与 `docs/superpowers/plans/*` 仅保留历史参考价值
- 代码注释、README、prompt、脚本、AI 工作流文档不得重新定义已批准的业务规则
- 若 `specs/README.md` 或 `specs/migration-map.md` 与具体 feature spec 冲突，以具体 feature spec 为准

### III. Baseline Then Split
`specs/hill-commerce-mvp/` 是当前商城 MVP 的 canonical baseline，用于承载全局范围和跨模块约束，但不能长期膨胀为万能大 spec。

当某个模块满足任一条件时，应拆出独立 feature spec：

- 有独立状态机
- 涉及金额、库存、权限、外部回调或其他高风险业务
- 有独立 API 分组
- 有独立数据模型或数据库表
- 需要 5 个以上可独立执行的 implementation tasks
- 会被长期单独迭代
- 规则复杂到无法在 baseline 中清晰表达

### IV. Evidence Before Drift
代码不是 canonical specification source，但代码可以作为迁移期间的事实发现来源。

当旧文档与当前代码不一致时：

1. 不默认代码正确
2. 不默认旧文档正确
3. 在 `specs/migration-map.md` 中记录冲突
4. 标记为 `needs-decision`
5. 由人工决定是更新 spec、修正代码，还是保留为已知差异
6. 在决策完成前，不把冲突内容写死进 canonical spec

### V. Scaled Spec Governance
规范治理采用分级策略，而不是把所有改动都升级为独立 spec。

必须先有 spec 的变更：

- 支付、订单、库存、优惠券、退款/售后
- 权限/鉴权
- 数据库 schema
- 状态机
- 金额计算
- 外部回调
- 并发、事务、幂等
- 会影响多个模块的架构调整

可不创建独立 spec 的低风险变更：

- UI 样式微调
- 文案修改
- loading 状态
- 简单 bugfix
- 变量重命名
- 小范围代码清理
- 不改变业务行为的 refactor

但低风险变更不得：

- 改变已有 `specs/*` 定义的行为
- 引入新的产品规则
- 覆盖或绕过 canonical spec

### VI. Spec Lifecycle
spec 生命周期必须显式可见，并在文件头部或对应 README 中标记状态。

允许的生命周期状态：

- `active`: 可定义当前产品行为、业务规则、架构边界与验收意图
- `deprecated`: 仍可保留迁移参考价值，但不得继续新增行为规则
- `superseded`: 已被新的 canonical spec 取代，且必须明确指向 replacement spec
- `archived`: 仅保留历史价值，不再定义当前行为

生命周期治理规则：

- 若某个 spec 与 active spec 冲突，以 active spec 为准
- `migration-map` 可以记录状态变更、替代关系和迁移来源，但不能替代 active spec 本身
- 历史 superpowers spec / plan 迁移后默认不再视为 active spec
- 生命周期状态必须有可读的替代关系说明，避免形成隐性双真相

## Project Constraints

- 当前项目是前后端分离的商城 MVP，架构采用模块化单体
- 首版主链路不得依赖 Redis 或 RocketMQ 才能运行
- 后端保持认证事实源，前端不自行定义认证真相
- 金额、库存、权限、状态流转、外部回调相关变更默认视为高风险
- 新规范优先落到 `specs/hill-commerce-mvp/`，只有满足拆分条件时才新建独立 feature 目录

## Governance

本文件高于仓库内其他流程说明、prompt 文档与历史 superpowers 规范文档。

`AGENTS.md` 可以承载 agent / workflow 执行细则，但不得覆盖本文件中的治理原则与优先级。

修订本文件时必须同时检查：

- `specs/README.md` 是否仍与之兼容
- `specs/hill-commerce-mvp/*` 是否需要同步更新
- `AGENTS.md` 是否仍指向正确的 canonical plan

治理修订要求：

- 原则新增或治理范围实质扩展，按 MINOR 版本升级
- 原则含义被重定义或出现不兼容变更，按 MAJOR 版本升级
- 仅澄清措辞、不改变实际约束时，按 PATCH 版本升级

**Version**: 1.1.0 | **Ratified**: 2026-05-11 | **Last Amended**: 2026-05-11
