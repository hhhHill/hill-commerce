# hill-commerce

Maven 多模块单体项目骨架，服务于 `hill-commerce`。

## Modules

- `apps/backend-api`
- `platforms/common-core`
- `platforms/common-web`
- `platforms/common-security`
- `platforms/common-mysql`
- `platforms/common-redis`
- `platforms/common-event`
- `platforms/common-test`
- `modules/user`
- `modules/auth`
- `modules/product`
- `modules/cart`
- `modules/order`
- `modules/analytics`
- `modules/recommendation`

## Build

```bash
mvn -q -DskipTests validate
```
