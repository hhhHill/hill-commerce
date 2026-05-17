# Feature Specification: scheduler-separation

**Feature**: `scheduler-separation`
**Status**: active

## Purpose

将 `@Scheduled` 定时调度逻辑从 Service 层拆出，为每个模块新建与 `service/` 平级的 `scheduler/` 层，统一管理定时任务触发。

## Scope

### In Scope

- 从 `PaymentCloseService` 拆出 `PaymentCloseScheduler`
- 从 `ShipmentService` 拆出 `ShipmentScheduler`
- 将 `AdminAnalyticsScheduler` 从 `modules/admin/service/` 移至 `modules/admin/scheduler/`
- 三个 Scheduler 类仅做调度委托，不含业务逻辑

### Out of Scope

- 修改调度频率或配置属性值
- 修改 `@EnableScheduling` 声明位置
- 新增或删除定时任务
- 修改任何业务逻辑

## Target Structure

```
modules/payment/
├── scheduler/
│   └── PaymentCloseScheduler.java      NEW — @Scheduled 定时扫描超时未支付订单
├── service/
│   └── PaymentCloseService.java        删掉 scheduledCloseExpiredPayments()
├── mapper/
├── entity/
├── web/

modules/order/
├── scheduler/
│   └── ShipmentScheduler.java          NEW — @Scheduled 定时扫描超时可自动完成订单
├── service/
│   └── ShipmentService.java            删掉 scheduledAutoComplete()
├── mapper/
├── entity/
├── web/

modules/admin/
├── scheduler/
│   └── AdminAnalyticsScheduler.java    MOVED FROM modules/admin/service/
├── service/
│   ├── AnomalyDetectionService.java
│   ├── AdminDashboardService.java
│   └── ...
```

## Behavior Rules

- 每个 Scheduler 类标注 `@Service`，注入对应业务 Service
- `@Scheduled` 方法体仅一行委托：`xxxService.businessMethod()`
- 不在 Scheduler 层加 `@Transactional`——事务由业务 Service 层控制
- Controller 对 Service 业务方法的调用完全不变
- `PaymentCloseService.closeExpiredPayments()` 和 `ShipmentService.autoComplete()` 保留为 public 方法，供 Scheduler 和 Controller 共用

## Risk Assessment

| 风险 | 缓解 |
|---|---|
| 新 Scheduler 未被 Spring 扫描到 | 新类与已有 Service 同模块、同组件扫描路径，`@Service` 确保被 Spring 管理 |
| 移动 `AdminAnalyticsScheduler` 导致 import 失效 | Grep 确认无任何文件显式 import 该类，Spring 组件扫描自动发现，移动包路径不破坏编译依赖 |
| `@Scheduled` 方法签名不一致 | 新 Scheduler 的方法签名与原 Service 中的完全一致，仅所属类不同 |
