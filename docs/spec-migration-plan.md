# Spec 迁移方案

## 1. 背景与当前现状

当前仓库已经存在一批有价值的需求与规划资产，但这些内容还没有收敛到单一的、符合 Spec Kit 预期的规范结构中。

已确认的当前状态如下：

- 历史产品/设计知识位于 `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md`
- 历史实施规划位于 `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md`
- Spec Kit 工具链已经安装在 `.specify/` 与 `.agents/skills/speckit-*`
- `.specify/memory/constitution.md` 仍然是占位模板，不是项目可用的 constitution
- `specs/` 目录目前还不存在
- 仓库已经有实现代码，但当代码与需求或架构设计冲突时，代码不能被视为规范事实源

这会带来三个直接风险：

1. 历史知识资产已经存在，但目录形态不符合 Spec Kit 的规范预期。
2. 当前 superpowers 文档把产品范围、架构、数据模型、API 形状和执行计划混在一起，如果直接照搬，会导致重复。
3. 如果在旧资产尚未规范化前就开始使用 Spec Kit 生成新 spec，仓库会出现并行 specification 树，失去单一事实源。

## 2. 迁移目标

本次迁移应达成以下目标：

- 让 `specs/*` 成为唯一的 canonical specification layer
- 保留当前 MVP 的知识资产，而不是重新生成一份
- 避免 `docs/superpowers/*`、`.specify/*` 与未来 `specs/*` 之间形成双重事实源
- 让 superpowers 聚焦于 prompts、workflows、commands、automation、execution helpers
- 让迁移过程保持渐进式、低风险，并适合多 agent 协作

## 3. 推荐的目标结构

建议的最终目录结构如下：

```text
project/
├── .specify/
│   ├── memory/
│   │   └── constitution.md
│   ├── templates/
│   └── scripts/
│
├── specs/
│   ├── README.md
│   ├── migration-map.md
│   └── hill-commerce-mvp/
│       ├── spec.md
│       ├── plan.md
│       └── tasks.md
│
├── docs/
│   └── superpowers/
│       ├── prompts/
│       ├── workflows/
│       ├── commands/
│       ├── checklists/
│       ├── specs/
│       └── plans/
│
├── AGENTS.md
└── src/
```

各部分设计意图如下：

- `.specify/memory/constitution.md`：项目长期原则，作为最高层规范约束
- `.specify/templates/`、`.specify/scripts/`：Spec Kit 的模板与脚本基础设施
- `specs/README.md`：给人和 agent 的统一入口，定义阅读顺序、命名规则、更新规则
- `specs/migration-map.md`：记录历史文档到 canonical specs 的来源映射与迁移决策
- `specs/hill-commerce-mvp/`：当前项目级 MVP 基线规范，用于承接现有全局范围设计
- `specs/<feature-name>/`：当某个模块、子系统或功能复杂到需要独立规范生命周期时，再按需创建；不是预先批量创建
- 每个 active feature 目录内固定三件套：`spec.md`、`plan.md`、`tasks.md`
- `docs/superpowers/prompts/`：执行任务、review、debug、commit message 等 prompt 资产
- `docs/superpowers/workflows/`：feature 开发、bugfix、refactor 等 AI 工作流
- `docs/superpowers/commands/`：命令入口、自动化调用说明、执行辅助文档
- `docs/superpowers/checklists/`：支付、订单、安全等风险检查清单
- `docs/superpowers/specs/` 与 `docs/superpowers/plans/`：迁移前历史规范资产；迁移后保留为历史参考
- `AGENTS.md`：所有 AI agent 的仓库协作规则
- `src/`：实现代码，不承载规范事实源

这个结构把 canonical spec 层、Spec Kit 工具层、superpowers 执行辅助层和实现层明确分开，更适合长期维护和多 agent 协作，同时避免把“规范迁移”和“superpowers 目录重构”混在同一次迁移里。

## 4. Source-of-Truth 规则

建议的 canonical 优先级如下：

1. `.specify/memory/constitution.md`
2. `specs/<feature>/spec.md`
3. `specs/<feature>/plan.md`
4. `specs/<feature>/tasks.md`
5. `specs/README.md`
6. `specs/migration-map.md`
7. `docs/superpowers/` 下的 prompts、workflows、commands、checklists 等执行辅助文档
8. `docs/superpowers/specs/*` 与 `docs/superpowers/plans/*` 下已经废弃的历史文档
9. implementation code

规则如下：

- `.specify/memory/constitution.md` 定义项目级长期原则，位于所有 feature spec 之上
- `specs/<feature>/spec.md` 是功能事实源，定义该 feature 的业务边界、规则、范围与验收意图
- `specs/<feature>/plan.md` 是技术执行方案，承接架构、模块边界、实现策略与关键技术约束
- `specs/<feature>/tasks.md` 是任务拆解，承接当前可执行实现视图
- `specs/README.md` 只是规范入口、阅读顺序和命名规则，不定义具体业务事实
- `specs/migration-map.md` 只是迁移溯源和决策记录，不应高于 active feature spec
- 如果 `specs/README.md` 或 `specs/migration-map.md` 与具体 `specs/<feature>/spec.md` 冲突，以具体 feature spec 为准
- 产品需求、范围边界、业务规则、架构决策、数据模型意图、验收标准，只能存在于 `specs/*`
- `.specify/*` 中除 `.specify/memory/constitution.md` 外，其余内容主要是工具与工作流脚手架，不是长期产品事实源
- `docs/superpowers/*` 定义的是 agent 如何执行，不定义产品是什么
- `docs/superpowers/*` 在迁移完成后转为历史参考，不再承载 canonical 更新
- 代码不是 canonical specification source
- 但迁移期间，代码可以作为发现当前实际行为、历史文档过时情况和文档/实现偏差的证据
- 如果 agent 在 `specs/*` 之外发现冲突信息，必须指出冲突，并以 `specs/*` 为准
- 当旧 spec、旧 plan 与当前代码不一致时，处理流程应为：
1. 不默认代码正确
2. 不默认旧文档正确
3. 在 `specs/migration-map.md` 或迁移备注中记录冲突
4. 标记为 `needs-decision`
5. 由人工决定是更新 spec、修正代码，还是保留为已知差异
6. 在决策完成前，不要把冲突内容固化为 canonical spec

## 5. Superpowers 与 Spec Kit 的职责划分

建议的职责划分如下：

### Spec Kit 负责

- Canonical feature specifications
- Canonical plans、tasks
- Feature 级 specification governance
- 通过 `.specify/memory/constitution.md` 承载项目长期原则
- 迁移映射、review gates 与规范生命周期

### Superpowers 负责

- Prompting workflows
- Planning 与 implementation execution patterns
- Agent orchestration
- Review / debugging / testing workflows
- Command wrappers 与 automation helpers
- 风险清单、执行清单、辅助 prompt
- 针对 canonical specs 的执行性工作指导
- 继续驻留在 `docs/superpowers/` 下，不在本次迁移中变更根目录

### 迁移后 superpowers 不再负责

- Authoritative product requirements
- Authoritative system behavior definitions
- Authoritative architecture decisions
- 长期存在的 feature specs 或 feature plans

补充约束：

- 本次迁移只处理 canonical spec/plan/tasks 的归属
- 不移动 `docs/superpowers/` 根目录
- 不把“canonical spec 迁移”和“superpowers 目录重构”混为一次改造
- `docs/superpowers/` 后续仍可承载 prompts、workflows、commands、automation、execution helpers、checklists
- `docs/superpowers/specs/*` 与 `docs/superpowers/plans/*` 在迁移后只作为历史参考，不再作为 canonical source

## 6. 旧文件到新文件的映射关系

### 主映射表

| 现有文件 | 目标 canonical 文件 | 迁移方式 | 说明 |
| --- | --- | --- | --- |
| `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md` | `specs/hill-commerce-mvp/spec.md` | `migrate + normalize` | 先承接当前全局 MVP 范围、角色、业务规则、非目标、验收标准 |
| `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md` | `specs/<feature-name>/spec.md` | `split later` | 当某个模块或功能复杂到需要独立规范生命周期时，再从 MVP 基线中拆出 |
| `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md` | `specs/hill-commerce-mvp/plan.md` | `migrate + normalize` | 先保留现有执行顺序与架构假设 |
| `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md` | `specs/<feature-name>/plan.md` | `split later` | 当对应 feature 独立出来后，再拆分为独立技术方案 |
| `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md` | `specs/hill-commerce-mvp/tasks.md` | `merge later` | 先保留为基线任务视图，后续再按 feature 拆分任务 |
| `README.md` | `specs/hill-commerce-mvp/plan.md` | `reference selectively` | 只抽取会影响架构或实现决策的内容作为引用、摘要或约束；不整体迁移 |
| `.specify/memory/constitution.md` | `.specify/memory/constitution.md` | `replace placeholder` | 保持原路径不变，把占位模板升级为真实项目 constitution |

### 当前 MVP 设计文档的章节级映射

| 当前章节 | 建议目标文件 |
| --- | --- |
| 项目简介、MVP 目标、设计原则 | `specs/hill-commerce-mvp/spec.md` |
| 角色、权限、功能范围、非目标 | `specs/hill-commerce-mvp/spec.md` |
| 页面清单、验收标准 | `specs/hill-commerce-mvp/spec.md` |
| 某个复杂模块或功能的独立业务规则 | 当该模块或功能需要独立生命周期时，拆入对应 `specs/<feature-name>/spec.md` |
| 模块结构与技术边界 | `specs/hill-commerce-mvp/plan.md` |
| 数据库表、字段约束、实体关系 | 先保留在 `specs/hill-commerce-mvp/spec.md` 或 `plan.md` 的相关章节，待 feature 边界稳定后再拆分 |
| API 分组与端点清单 | 先保留在 `specs/hill-commerce-mvp/spec.md` 或 `plan.md` 的相关章节，后续按 feature 拆分 |
| 状态流转附录 | 根据归属，在需要独立化时拆入对应 `specs/<feature-name>/spec.md` |
| 任务拆分 | 先进入 `specs/hill-commerce-mvp/tasks.md`，后续按 feature 切分 |

## 7. 文件处理分类

### 应迁移（migrate）

- `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md`
- `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md`

### 应合并（merge）

- 旧 design 与旧 plan 中重复出现的规则，收敛进 `specs/hill-commerce-mvp/spec.md` 或 `plan.md`

### 应引用/摘要（reference selectively）

- `README.md` 不整体迁移
- `README.md` 继续作为项目介绍、本地启动、安装、环境说明、常用命令等开发入口
- 只抽取 `README.md` 中会影响架构或实现决策的内容，作为 `specs/hill-commerce-mvp/plan.md` 的引用、摘要或约束
- 不要把安装命令、本地启动命令、环境变量样例、普通开发说明复制进 `plan.md`
- 如果需要承载开发手册，应保留在 `README.md` 或单独的 `docs/development.md` / `docs/setup.md`，而不是塞进 `plan.md`

### 应废弃（deprecate）

- `docs/superpowers/specs/*` 作为新 canonical specs 的存放位置
- `docs/superpowers/plans/*` 作为新 canonical implementation plans 的存放位置
- 不要新建 `.specify/constitution.md`
- 继续使用 `.specify/memory/constitution.md` 作为 constitution 的唯一有效路径

### 应归档（archive）

- `docs/superpowers/specs/*` 与 `docs/superpowers/plans/*` 作为历史资产保留原位，待迁移稳定后标记为只读历史档案
- 第一阶段不建议删除任何旧文件

### Deprecated Notice 模板

迁移完成后，`docs/superpowers/specs/*` 与 `docs/superpowers/plans/*` 顶部应添加如下 notice：

> Deprecated canonical source
>
> This document has been migrated to:
> - specs/hill-commerce-mvp/spec.md
> - specs/hill-commerce-mvp/plan.md
> - specs/hill-commerce-mvp/tasks.md
>
> Do not update this file as a source of truth.
> It is preserved only for historical context.

如果后续拆出了独立 feature，应把 notice 中的目标路径替换为对应 `specs/<feature>/...` 文件。

## 8. 推荐的迁移策略

本次迁移建议分两层进行。

### 第一层：canonical bootstrap

- 保持 `.specify/memory/constitution.md` 原路径不变
- 创建 `specs/README.md` 与 `specs/migration-map.md`
- 创建 `specs/hill-commerce-mvp/`
- 手工整理现有 MVP 内容到 `spec.md`、`plan.md`、`tasks.md` 三件套中
- 保持语义不变，不为了贴模板而重写需求
- 在 `specs/migration-map.md` 中记录 provenance

### 第二层：workflow cutover

- 停止在 `docs/superpowers/` 下创建新的 canonical 文档
- 把 agent 工作流与人工协作入口都指向 `specs/*`
- 更新 AGENTS 上下文与 constitution 引用，使其指向 `.specify/memory/constitution.md` 与相关 `specs/<feature>/plan.md`
- 保留 superpowers，但只把它作为 canonical specs 之上的执行 machinery

重要非目标：

- 不要把已经写好的 MVP 基线当成一个“新 feature 描述”再运行 `/speckit-specify`。那会产生第二套 MVP spec，而不是迁移已有资产。
- 不要在本次迁移中移动 `docs/superpowers/` 根目录或重构其 prompts/workflows/checklists/commands 的位置

## 9. 潜在冲突与风险

### 已观察到的冲突

- 需求里提到的历史目录 `superpowers/spec/` 与 `superpowers/plan/` 在当前仓库并不存在；真实历史资产位于 `docs/superpowers/specs/` 与 `docs/superpowers/plans/`
- 当前 MVP design doc 不是一个纯粹的 Spec Kit `spec.md`，它混合了产品、架构、数据库、API 和执行拆解
- 当前 implementation plan 中包含了一部分也出现在 design doc 中的架构事实，存在重复
- `.specify/memory/constitution.md` 仍是占位模板，因此如果现在直接依赖它来跑 Spec Kit planning，结果会偏弱甚至误导

### 迁移风险

- 如果直接把旧文档机械复制进新结构，会把决策散落并重复到多个 feature 的 `spec.md`、`plan.md`、`tasks.md` 中
- 如果迁移未完成就开始生成新 spec，会出现双 source-of-truth
- 如果默认“代码已经实现了，所以代码就是对的”，会把偶然行为固化为规范
- 如果过早抽取共享架构规则，容易在 `.specify/memory/constitution.md`、`specs/README.md` 与各 feature 目录之间再制造重复
- 如果没有明确 provenance 与优先级，多 agent 编辑会持续漂移
- 如果过早把复杂模块从 baseline 拆空，会让规范层碎片化，增加个人开发者维护成本

### 风险控制

- 采用带 provenance 的手工迁移，而不是重生成
- 从单一 canonical baseline 目录开始：`specs/hill-commerce-mvp/`
- 第一阶段保持最小顶层治理：`.specify/memory/constitution.md`、`specs/README.md`、`specs/migration-map.md`
- 只有当至少两个 feature spec 都需要同一规则时，才把交叉规则提升到 `.specify/memory/constitution.md` 或 `specs/README.md`
- 要求每个 agent 都明确引用自己依据的 `specs/...` 文件

## 10. hill-commerce-mvp 的定位与拆分触发条件

`specs/hill-commerce-mvp/` 的定位应明确如下：

- 它是当前商城 MVP 的 canonical baseline
- 适合承载项目目标、MVP 范围、角色、全局非目标、核心模块总览、跨模块约束
- 不应长期承载过细的模块规则、状态机、支付细节、库存锁定策略、优惠券叠加规则、退款细节等

当某个模块满足以下任意条件时，应从 `specs/hill-commerce-mvp/` 拆出独立 feature spec：

1. 有独立状态机
2. 涉及金额、库存、权限、外部支付回调或其他高风险业务
3. 有独立 API 分组
4. 有独立数据模型或数据库表
5. 需要 5 个以上可独立执行的 implementation tasks
6. 后续会被单独迭代
7. 业务规则复杂到无法在 MVP baseline 中清晰表达

商城项目中，未来优先考虑独立 spec 的模块包括：

- `order`
- `payment`
- `inventory`
- `cart`
- `checkout`
- `promotion`
- `refund-after-sales`
- `auth / permission`
- `admin-product-management`

## 11. AI Agent 后续应如何引用 spec

cutover 之后，agent 的推荐读取顺序如下：

1. `.specify/memory/constitution.md`
2. 目标 feature 的 `spec.md`
3. 目标 feature 的 `plan.md`
4. 目标 feature 的 `tasks.md`
5. `specs/README.md`
6. `specs/migration-map.md`
7. 如有需要，再回读 `specs/hill-commerce-mvp/` 作为全局基线或迁移中的项目级上下文

Agent 工作规则：

- 开始执行任务前，agent 必须明确指出自己使用的是哪个 feature 目录
- 如果任务跨多个 feature，agent 必须标出 primary feature spec，并列出 secondary references
- 如果 agent 只在 `docs/superpowers/*` 中找到相关信息，可以把它当作历史上下文，但不能把它当成高于 `specs/*` 的权威来源
- 采用分级 spec 策略：
1. 高风险或中高复杂度变更，必须先有 spec，例如支付、订单、库存、优惠券、退款/售后、权限/鉴权、数据库 schema、状态机、金额计算、外部回调、并发、事务、幂等、会影响多个模块的架构调整
2. 低风险变更可以不创建独立 spec，例如 UI 样式微调、文案修改、loading 状态、简单 bugfix、变量重命名、小范围代码清理、不改变业务行为的 refactor
3. 但低风险变更必须遵守：
- 不能改变已有 `specs/*` 定义的行为
- 不能引入新的产品规则
- 不能覆盖或绕过 canonical spec
- 如果实现过程中发现会影响业务规则，必须停止并补充或更新 spec
- 如果任务会修改全局规则，agent 必须在同一个变更集中更新 `.specify/memory/constitution.md` 或相关 canonical spec

## 12. Constitution 建议更新内容

新的 constitution 至少应增加以下原则：

1. Canonical specification principle
   所有规范性的产品、架构与行为定义都必须位于 `.specify/memory/constitution.md` 与 `specs/*`。

2. No dual truth principle
   历史文档、prompt、代码注释、工作流文件都不能在 `specs/*` 之外重新定义已批准需求。

3. Provenance principle
   迁移后的 spec 必须记录来源文档和关键解释性决策。

4. Amendment-before-divergence principle
   如果实现需要偏离 spec，必须先改 spec，或在同一个变更中同步修订 spec。

5. Multi-agent citation principle
   每个会影响行为的 agent 变更都必须标注其依据的 `specs/...` 路径。

6. Shared-rule extraction principle
   只有真正跨多个 feature 复用的规则才进入 `.specify/memory/constitution.md` 或 `specs/README.md`；否则保持在所属目录内。

7. Workflow/tooling separation principle
   `.specify/`、`.agents/` 和 `docs/superpowers/` 下的 workflows/prompts/commands/checklists 属于执行基础设施，不属于规范事实源。

## 13. Codex CLI 后续推荐工作流

### 面向现有基线工作

1. 先读 `.specify/memory/constitution.md`
2. 再读相关 feature 的 `spec.md`
3. 根据需要读取该 feature 的 `plan.md`、`tasks.md`
4. 再读 `specs/README.md`
5. 如有迁移上下文需求，再读 `specs/migration-map.md`
6. 如果该任务涉及全局约束，再回读 `specs/hill-commerce-mvp/*`
7. 基于这些文件执行实现或 review

### 面向新 feature

1. 通过 Spec Kit 在 `specs/` 下按需创建新的 feature 目录
2. 需要时先 clarify
3. 生成或整理 `plan.md`
4. 生成 `tasks.md`
5. 基于该 feature 目录执行实现
6. 只有在 feature 改动项目级长期原则时才更新 `.specify/memory/constitution.md`

### 面向 superpowers 的使用方式

- 使用 superpowers 驱动 brainstorming、planning discipline、debugging、execution、review、verification
- 继续使用 `docs/superpowers/` 存放 prompts、workflows、commands、checklists 等执行辅助文档
- 不要再使用 `docs/superpowers/specs/*` 与 `docs/superpowers/plans/*` 存放 canonical feature requirements
- 不要再在 `docs/superpowers/` 下创建新的长期产品 spec

## 14. 推荐的首个迁移单元

建议把当前 MVP baseline 作为第一个迁移单元，因为它是现有知识资产中范围最广、信息密度最高的一组：

- 创建一个 canonical baseline 目录：`specs/hill-commerce-mvp/`
- 用两份现有 superpowers 文档进行人工落盘，并只从 `README.md` 中抽取会影响架构或实现决策的约束
- 在启用常规 `/speckit-plan` 之前，先写好 `.specify/memory/constitution.md`
- 完成后再允许那些真正复杂、需要独立规范生命周期的模块或功能逐步进入独立 spec 生命周期

这样可以最大化保留历史上下文，降低破坏性，并避免制造第二条 MVP spec 线。

## 15. 迁移验收标准（Definition of Done）

满足以下条件时，可认为迁移方案执行完成：

- `specs/hill-commerce-mvp/spec.md` 已创建，并包含 MVP 目标、角色、范围、非目标、核心业务规则、验收标准
- `specs/hill-commerce-mvp/plan.md` 已创建，并包含技术架构、模块边界、数据模型意图、API 分组、关键技术约束
- `specs/hill-commerce-mvp/tasks.md` 已创建，并从旧 plan 中整理出当前可执行任务视图
- `specs/README.md` 已创建，并说明阅读顺序、命名规则、更新规则
- `specs/migration-map.md` 已创建，并记录旧文件到新 canonical 文件的迁移映射
- `.specify/memory/constitution.md` 已从占位模板替换为真实项目 constitution
- `docs/superpowers/specs/*` 和 `docs/superpowers/plans/*` 顶部已添加 deprecated notice
- `AGENTS.md` 或等效 agent 规则文件已明确 `specs/*` 是 canonical source
- `README.md` 没有被整体迁移，只抽取了会影响架构/实现决策的约束
- 没有生成第二套 MVP spec
- 新任务默认引用 `specs/*`，而不是 `docs/superpowers/specs/*` 或 `docs/superpowers/plans/*`

## 16. 决策总结

推荐决策如下：

- 使用 `.specify/memory/constitution.md` + `specs/*` 组成唯一 canonical specification layer
- 将 `docs/superpowers/*` 视为迁移输入与执行辅助层，迁移后其中的 `specs/*` 与 `plans/*` 转为历史档案
- 保留 `docs/superpowers/{prompts,workflows,commands,checklists}` 作为执行工具层，而不是 specification storage
- 用人工迁移而不是重新生成的方式，把当前 MVP 基线落到 `specs/hill-commerce-mvp/`
- 在开始常规按需 feature 化的 Spec Kit 使用前，先完善 `.specify/memory/constitution.md` 与 `specs/README.md`
