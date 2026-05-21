# Implementation Plan: scheduler-separation

**Feature**: `scheduler-separation`
**Status**: active

## Summary

三步纯重构：两个新 Scheduler 类 + 一个 Scheduler 移动 + 两个 Service 删除调度代码。不涉及任何业务逻辑变更、配置变更或测试调整。

## Current State

```
PaymentCloseService.java          @Scheduled + 业务逻辑混合
ShipmentService.java              @Scheduled + 业务逻辑混合
AdminAnalyticsScheduler.java      已在 service/ 包下，不在独立的 scheduler/ 层
```

## Target State

```
modules/payment/scheduler/PaymentCloseScheduler.java   @Scheduled 委托 → PaymentCloseService.closeExpiredPayments()
modules/order/scheduler/ShipmentScheduler.java         @Scheduled 委托 → ShipmentService.autoComplete()
modules/admin/scheduler/AdminAnalyticsScheduler.java   MOVED from modules/admin/service/

PaymentCloseService.java    仅保留业务逻辑（closeExpiredPayments + closeExpiredOrder）
ShipmentService.java        仅保留业务逻辑（autoComplete + shipOrder + confirmReceipt + ...）
```

## Step-by-Step

### Step 1: 新建 `PaymentCloseScheduler`

- 文件：`modules/payment/scheduler/PaymentCloseScheduler.java`
- 标注 `@Service`
- 注入 `PaymentCloseService`
- 将原 `PaymentCloseService.scheduledCloseExpiredPayments()` 的 `@Scheduled` 注解及方法签名搬过来
- 方法体：`paymentCloseService.closeExpiredPayments();`

### Step 2: 新建 `ShipmentScheduler`

- 文件：`modules/order/scheduler/ShipmentScheduler.java`
- 标注 `@Service`
- 注入 `ShipmentService`
- 将原 `ShipmentService.scheduledAutoComplete()` 的 `@Scheduled` 注解及方法签名搬过来
- 方法体：`shipmentService.autoComplete();`

### Step 3: 移动 `AdminAnalyticsScheduler`

- 从 `modules/admin/service/AdminAnalyticsScheduler.java`
- 移至 `modules/admin/scheduler/AdminAnalyticsScheduler.java`
- 更新 package 声明：`com.hillcommerce.modules.admin.service` → `com.hillcommerce.modules.admin.scheduler`
- 添加 import `com.hillcommerce.modules.admin.service.AnomalyDetectionService`（原来同包不需 import）

### Step 4: 清理 `PaymentCloseService`

- 删除 `scheduledCloseExpiredPayments()` 方法
- 删除 `import org.springframework.scheduling.annotation.Scheduled`
- 其余代码不变

### Step 5: 清理 `ShipmentService`

- 删除 `scheduledAutoComplete()` 方法
- 删除 `import org.springframework.scheduling.annotation.Scheduled`
- 其余代码不变

## Verification

- `mvn compile` 编译通过
- 运行现有集成测试 `PaymentIntegrationTest`、`FulfillmentIntegrationTest` 全部通过
- `@Scheduled` 方法在 Spring 启动后能被正确调度（通过日志确认）
