# Module Dependencies

- `apps/backend-api` can depend on `platforms/*` and `modules/*`
- `modules/*` can depend on `platforms/*` and other module `api`
- direct cross-module access to `domain` and `infrastructure` is forbidden
