# 本地开发说明

1. 在本地启动 MySQL 与 Redis。
2. 运行 `mvn -pl apps/backend-api -am spring-boot:run`。
3. 访问 `GET /api/v1/health` 验证服务状态。
