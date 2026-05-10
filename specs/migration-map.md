# Spec Migration Map

## Purpose

本文件记录历史 superpowers 文档到当前 canonical specs 的迁移映射、迁移范围、以及迁移过程中发现的冲突与待决策项。

本文件不是业务事实源；若与 active feature spec 冲突，以 active feature spec 为准。

## Canonical Targets

- `specs/hill-commerce-mvp/spec.md`
- `specs/hill-commerce-mvp/plan.md`
- `specs/hill-commerce-mvp/tasks.md`
- `.specify/memory/constitution.md`

## File Mapping

| Historical Source | Canonical Target | Mode | Notes |
| --- | --- | --- | --- |
| `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md` | `specs/hill-commerce-mvp/spec.md` | `migrate + normalize` | 承接 MVP 目标、角色、范围、非目标、核心业务规则、验收标准 |
| `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md` | `specs/<feature-name>/spec.md` | `split later` | 仅当复杂模块满足拆分条件时，才从 baseline 抽出 |
| `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md` | `specs/hill-commerce-mvp/plan.md` | `migrate + normalize` | 承接技术架构、模块边界、数据模型意图、API 分组、关键约束 |
| `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md` | `specs/hill-commerce-mvp/tasks.md` | `derive` | 提炼当前可执行任务视图 |
| `README.md` | `specs/hill-commerce-mvp/plan.md` | `reference selectively` | 只抽取影响架构或实现决策的约束，不整体迁移 |
| `.specify/memory/constitution.md` | `.specify/memory/constitution.md` | `replace placeholder` | 以项目真实 constitution 替换模板 |

## Deprecated Historical Sources

以下文件保留用于历史参考，但不再作为 canonical source：

- `docs/superpowers/specs/2026-05-09-hill-commerce-mvp-design.md`
- `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md`

## Needs-Decision Log

当前迁移初始化阶段尚未记录已确认的代码/文档冲突。

后续如果发现旧 spec、旧 plan 与代码不一致，使用以下格式追加：

```md
### MD-001

- Area: <topic>
- Historical source: <path>
- Code evidence: <path or behavior>
- Proposed canonical target: <path>
- Status: needs-decision
- Notes: <why unresolved>
```

## Migration Rules

- 不默认代码正确
- 不默认旧文档正确
- 冲突先记入本文件，再由人工决策
- 在决策完成前，不把冲突内容写死进 canonical spec
