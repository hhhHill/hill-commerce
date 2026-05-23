# CI/CD 环境搭建清单

一次性搭建步骤，按顺序执行。适用于新 VPS 或新开发者上手。

---

## 1. VPS 准备

### 1.1 安装 Docker

```bash
# Ubuntu / Debian
curl -fsSL https://get.docker.com | bash
sudo usermod -aG docker $USER
newgrp docker
docker --version
docker compose version
```

### 1.2 创建 deploy 用户与 SSH 密钥

```bash
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG docker deploy
sudo su - deploy

# 生成密钥对
ssh-keygen -t ed25519 -f ~/.ssh/github-actions -N ""
cat ~/.ssh/github-actions.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 输出私钥（复制到 GitHub Secrets → VPS_SSH_KEY）
cat ~/.ssh/github-actions
```

### 1.3 创建工作目录与 .env

```bash
sudo mkdir -p /opt/hill-commerce
sudo chown deploy:deploy /opt/hill-commerce
cd /opt/hill-commerce

# 创建 .env，填入 OSS/第三方密钥
cat > .env << 'EOF'
OSS_ACCESS_KEY_ID=<你的AK>
OSS_ACCESS_KEY_SECRET=<你的SK>
EOF
```

### 1.4 开放安全组

阿里云 / 腾讯云控制台 → 安全组 → 入方向规则，添加：

| 端口 | 协议 | 来源 | 说明 |
|------|------|------|------|
| 80 | TCP | 0.0.0.0/0 | HTTP |
| 443 | TCP | 0.0.0.0/0 | HTTPS（后续） |

---

## 2. 阿里云 ACR 配置

### 2.1 开通容器镜像服务

阿里云控制台 → 容器镜像服务 ACR → 个人版（免费）→ 创建命名空间 `hill-commerce`。

地域选 VPS 所在地（如深圳：`cn-shenzhen`），仓库类型选**公开**或**私有**均可。

### 2.2 设置访问凭证

ACR 控制台 → 访问凭证 → 设置固定密码。记录：

- **用户名**：页面上显示的用户名（非阿里云账号密码）
- **密码**：刚设置的固定密码

### 2.3 验证本地登录

```bash
docker login crpi-<实例ID>.cn-<地域>.personal.cr.aliyuncs.com -u <ACR用户名>
# 输入固定密码
```

---

## 3. GitHub Secrets 配置

仓库 → Settings → Secrets and variables → Actions → New repository secret：

| Secret | 说明 | 示例 |
|--------|------|------|
| `VPS_HOST` | VPS 公网 IP | `47.xxx.xxx.xxx` |
| `VPS_USER` | SSH 用户名 | `deploy` |
| `VPS_SSH_KEY` | SSH 私钥（完整多行） | 第一步 `cat` 的输出 |
| `ACR_REGISTRY` | ACR 实例地址 | `crpi-xxx.cn-shenzhen.personal.cr.aliyuncs.com` |
| `ACR_NAMESPACE` | ACR 命名空间 | `hill-commerce` |
| `ACR_USERNAME` | ACR 访问凭证用户名 | ACR 控制台显示的用户名 |
| `ACR_PASSWORD` | ACR 访问凭证固定密码 | 第二步设置的密码 |

> **注意**：`VPS_SSH_KEY` 粘贴时必须保留换行符，用 VS Code 或网页直接粘贴，**不要用记事本**。

---

## 4. 首次部署验证

推送代码触发 CI/CD，或手动重跑 workflow。部署完成后在 VPS 上确认：

```bash
docker ps | grep hill-commerce
# 应该看到 nginx、frontend、backend 三个容器都在 Up
```

浏览器访问 `http://<VPS_IP>` 确认页面加载正常。

---

## 5. 本地开发

本地开发不需要 CI/CD，直接用 docker compose：

```bash
# 仅启基础设施
docker compose up -d

# 启全栈（前后端本地构建）
docker compose --profile app up -d --build
```
