# 模块依赖规则

- `apps/backend-api` 可以依赖 `platforms/*` 与 `modules/*`
- `modules/*` 可以依赖 `platforms/*` 与其他模块的 `api`
- 禁止跨模块直接访问 `domain` 与 `infrastructure`
