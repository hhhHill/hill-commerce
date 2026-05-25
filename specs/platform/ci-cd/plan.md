# CI/CD Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 CI workflow 中新增 CD job，push main 后自动构建 Docker 镜像推送到 GHCR 并部署到 VPS。

**Architecture:** 单文件改动——修改 `.github/workflows/ci.yml`，在现有 backend/frontend 两个 CI job 后新增 deploy job，串行执行：测试通过 → 镜像构建推送 → SSH 部署。

**Tech Stack:** GitHub Actions, Docker Buildx, GHCR, appleboy/scp-action, appleboy/ssh-action

---

### Task 1: 新增 CD deploy job 到 CI workflow

**Files:**
- Modify: `.github/workflows/ci.yml`（在 frontend job 后追加 deploy job）

- [ ] **Step 1: 修改 ci.yml，添加 deploy job**

在现有 jobs 末尾（`frontend` job 结束行之后）追加：

```yaml
  deploy:
    name: Deploy to VPS
    runs-on: ubuntu-latest
    needs: [backend, frontend]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push backend image
        uses: docker/build-push-action@v6
        with:
          context: ./backend
          file: ./backend/Dockerfile
          push: true
          tags: ghcr.io/${{ github.repository_owner }}/hill-commerce-backend:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Build and push frontend image
        uses: docker/build-push-action@v6
        with:
          context: ./frontend/next-app
          file: ./frontend/next-app/Dockerfile
          push: true
          tags: ghcr.io/${{ github.repository_owner }}/hill-commerce-frontend:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Copy docker-compose.yml to VPS
        uses: appleboy/scp-action@v0
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          source: docker-compose.yml
          target: /opt/hill-commerce/

      - name: Deploy on VPS
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
            docker pull ghcr.io/${{ github.repository_owner }}/hill-commerce-backend:latest
            docker pull ghcr.io/${{ github.repository_owner }}/hill-commerce-frontend:latest
            cd /opt/hill-commerce
            docker compose up -d --remove-orphans
```

- [ ] **Step 2: 提交 workflow 变更**

```bash
git add .github/workflows/ci.yml
git commit -m "feat: add CD deploy job — build Docker images, push to GHCR, SSH deploy to VPS"
```

---

### Task 2: VPS 准备（一次性手动操作，不在仓库内）

以下操作在 VPS 上手动执行：

- [ ] **Step 1: 创建 deploy 用户和 SSH 密钥**

```bash
# 在 VPS 上
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG docker deploy
sudo su - deploy
ssh-keygen -t ed25519 -f ~/.ssh/github-actions -N ""
cat ~/.ssh/github-actions.pub >> ~/.ssh/authorized_keys
cat ~/.ssh/github-actions  # 复制私钥内容，存到 GitHub Secrets: VPS_SSH_KEY
```

- [ ] **Step 2: 创建工作目录和 .env**

```bash
# 在 VPS 上
sudo mkdir -p /opt/hill-commerce
sudo chown deploy:deploy /opt/hill-commerce
cd /opt/hill-commerce
# 创建 .env 文件，填入 OSS 等密钥
```

- [ ] **Step 3: 配置 GitHub Secrets**

在 GitHub 仓库 Settings → Secrets and variables → Actions 中添加：
- `VPS_HOST`: VPS 公网 IP
- `VPS_USER`: `deploy`
- `VPS_SSH_KEY`: 上一步复制的私钥内容

### Task 3: 验证

- [ ] **Step 1: 推送 test 分支触发生成流程**

推送代码到 main，观察 GitHub Actions 运行结果。确认 CI 通过后 CD job 执行，VPS 上容器更新。

- [ ] **Step 2: 验证 VPS 上的容器**

```bash
ssh deploy@<vps-ip>
cd /opt/hill-commerce
docker compose ps
docker compose logs --tail=20
```

---

### 自检清单

1. **Spec 覆盖**: spec 中所有章节（镜像策略、Secrets、CD 流程、VPS 准备、失败处理）均有对应任务覆盖。
2. **无占位符**: 所有代码均为完整实现，无 TBD/TODO。
3. **类型一致性**: 镜像名、secret 名、文件路径在各步骤中一致。
