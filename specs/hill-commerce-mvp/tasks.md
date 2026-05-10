# Tasks: hill-commerce-mvp

**Feature**: `hill-commerce-mvp`  
**Status**: Current Executable View  
**Source**: Derived from `docs/superpowers/plans/2026-05-09-hill-commerce-mvp.md`

## Usage

本文件提供当前商城 MVP 的任务视图，供后续实现和拆分 feature 时引用。

说明：

- 这是规范层任务视图，不等同于一次性全部立即实施
- 当某一任务主题复杂度提升时，应先拆出独立 feature spec / plan / tasks
- 高风险主题如 `order`、`payment`、`inventory`、`auth-permission` 默认优先考虑独立化

## Milestone A: 基础骨架与权限闭环

- [ ] 任务 1：工程骨架与基础设施
  建立前后端工程基线、环境配置、缓存与事件抽象预留、Compose 与 Nginx 占位。

- [ ] 任务 2：数据库基线与迁移体系
  固化首版表结构、角色种子、管理员种子及 Flyway 回归检查方式。

- [ ] 任务 3：认证、会话与权限体系
  完成注册、登录、注销、角色边界、受保护页面、日志记录。

- [ ] 任务 4：后台商品管理
  完成一级分类、SPU、SKU、销售属性、展示属性、图片与商品状态管理。

## Milestone B: 前台购物与交易闭环

- [ ] 任务 5：前台商品浏览与搜索
  完成首页、商品列表、商品详情、搜索与商品浏览日志。

- [ ] 任务 6：购物车与地址管理
  完成购物车增删改查、地址管理、默认地址与结算前校验提示。

- [ ] 任务 7：下单、库存扣减与订单快照
  完成结算校验、订单创建、订单项快照、库存扣减、手动取消与事件发布抽象调用。

- [ ] 任务 8：模拟支付与超时关闭
  完成支付流水、支付成功状态流转、超时关闭、库存回补与幂等处理。

## Milestone C: 履约、审计与统计

- [ ] 任务 9：发货、物流、确认收货与自动完成
  完成后台订单、发货录入、前台查单、确认收货、自动完成。

- [ ] 任务 10：日志、邮件、基础统计与后台审计
  完成登录日志、操作日志、浏览日志、邮件通知、Dashboard 汇总。

- [ ] 任务 11：联调、异常处理与上线准备
  完成主流程联调、异常页、部署说明、完整演示链路验证。

## Recommended First Split Candidates

以下任务在继续展开前，建议优先考虑独立 feature spec：

- [ ] `auth-permission`
- [ ] `admin-product-management`
- [ ] `cart`
- [ ] `checkout`
- [ ] `order`
- [ ] `payment`
- [ ] `inventory`
- [ ] `refund-after-sales`

## Task Dependencies

- 任务 1 是所有后续任务前置
- 任务 2、任务 3 依赖任务 1
- 任务 4 依赖任务 3 的后台权限边界
- 任务 5 依赖任务 4 的商品可用数据
- 任务 6 依赖任务 3、任务 5
- 任务 7 依赖任务 4、任务 6
- 任务 8 依赖任务 7
- 任务 9 依赖任务 8
- 任务 10 可穿插推进，但依赖前序能力逐步齐备
- 任务 11 最后进行

## Future Extraction Rule

若某一任务出现以下情况，应停止继续在 baseline 内细化，并先拆独立 feature：

- 出现独立状态机
- 涉及金额、库存、权限、外部回调
- 出现独立 API 组和独立数据模型
- 子任务超过 5 个且能独立执行
- 后续会被单独长期迭代
