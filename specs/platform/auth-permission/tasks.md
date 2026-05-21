# Tasks: auth-permission

**Status**: active

## Goal

完成认证、会话与权限边界闭环。

## Tasks

- [ ] 补齐注册、登录、注销、`me` 的后端行为约束
- [ ] 将密码哈希策略固化为单一规则：运行期仅接受 `BCrypt`
- [ ] 确认角色模型与种子数据使用方式
- [ ] 固化 `/account`、`/admin`、`/forbidden` 的访问规则
- [ ] 固化前端同源代理与 middleware 的职责边界
- [ ] 增加登录成功/失败日志验收点
- [ ] 定义该 feature 的验证命令与人工回归路径
- [ ] 固化成功登录后的状态更新：写入 `lastLoginAt`
- [ ] 移除认证链路中的无盐 `SHA-256` 兼容校验
- [ ] 将本地 bootstrap 管理员种子从 `SHA2(...)` 或等价弱哈希改为 `BCrypt` 哈希值
- [ ] 调整自动化测试基线：`BCrypt` 账号登录成功，无盐 `SHA-256` 账号登录失败
- [ ] 如发现历史弱哈希账号，定义失效后重置密码流程，不引入透明迁移或兼容窗口
- [ ] 将认证阶段 principal 与会话阶段 principal 拆分，避免 `passwordHash` 进入 `HttpSession`
- [ ] 调整 `/login`、`/me` 和后台权限读取路径，使其依赖会话期最小身份视图
- [ ] 增加回归测试：登录成功后 `SPRING_SECURITY_CONTEXT` 中的 principal 不持有 `passwordHash`
- [ ] 增加回归测试：成功登录更新 `lastLoginAt`，失败登录不更新 `lastLoginAt`

## Done When

- 用户与角色边界行为可独立验收
- 后续业务 feature 不再重新定义认证真相
- 密码哈希策略在种子、注册、认证、测试之间不存在双重规则
- 仓库中不再把无盐 `SHA-256` 账号登录成功视为正确行为
- Session 生命周期中的 principal 不再保留 `passwordHash` 或等价敏感凭据材料
- `lastLoginAt` 能准确反映最近一次成功登录，而不是保持空值或被失败登录污染
