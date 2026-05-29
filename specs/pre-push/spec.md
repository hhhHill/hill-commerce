# Pre-Push 检查清单

## 后端

```bash
# 进入后端目录
cd backend

# 跑所有测试
mvn test

# 如果只想跑某个模块（加速）：
mvn test -pl .  # 全量
mvn test -Dtest="AuthFlowIntegrationTest"  # 单个测试类
```

环境要求：本地需要 MySQL 和 Redis。如果本地没有，可以用 Docker 快速启动：

```bash
docker run -d --name mysql-test \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=hill_commerce \
  -e MYSQL_USER=hill \
  -e MYSQL_PASSWORD=hill123 \
  -p 3306:3306 \
  mysql:9.7.0

docker run -d --name redis-test -p 6379:6379 redis:8.0
```

## 前端

```bash
cd frontend/next-app

# 1. 类型检查
npx tsc --noEmit

# 2. 单元测试
npx vitest run

# 3. 验证构建
npm run build
```

## 一键检查脚本

项目根目录下可以跑：

**Windows (PowerShell):**
```powershell
Write-Host "=== Backend Tests ===" -ForegroundColor Cyan
Set-Location backend
mvn -B test
if ($LASTEXITCODE -ne 0) { Write-Host "BACKEND FAILED" -ForegroundColor Red; exit 1 }
Set-Location ..

Write-Host "=== Frontend TypeCheck ===" -ForegroundColor Cyan
Set-Location frontend/next-app
npx tsc --noEmit
if ($LASTEXITCODE -ne 0) { Write-Host "TYPECHECK FAILED" -ForegroundColor Red; exit 1 }

Write-Host "=== Frontend Tests ===" -ForegroundColor Cyan
npx vitest run
if ($LASTEXITCODE -ne 0) { Write-Host "FRONTEND TESTS FAILED" -ForegroundColor Red; exit 1 }
Set-Location ../..

Write-Host "ALL PASS" -ForegroundColor Green
```

**Linux/Mac:**
```bash
set -e
echo "=== Backend Tests ==="
(cd backend && mvn -B test)

echo "=== Frontend TypeCheck ==="
(cd frontend/next-app && npx tsc --noEmit)

echo "=== Frontend Tests ==="
(cd frontend/next-app && npx vitest run)

echo "ALL PASS"
```

## CI 对照表

本地命令和 CI 里的一一对应：

| CI Step | 本地等价命令 |
|---------|-------------|
| `mvn -B test` | `cd backend && mvn test` |
| `tsc --noEmit` | `cd frontend/next-app && npx tsc --noEmit` |
| `vitest run` | `cd frontend/next-app && npx vitest run` |

三条全绿就可以 push，不需要等 CI。
