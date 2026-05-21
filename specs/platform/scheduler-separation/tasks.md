# Tasks: scheduler-separation

**Status**: active

## Goal

将 `@Scheduled` 调度代码从 Service 层拆至独立的 `scheduler/` 层，统一三个模块的调度模式。

## Implementation Order

### Phase 1: 新建 Scheduler 类

- [X] 1.1 新建 `backend/src/main/java/com/hillcommerce/modules/payment/scheduler/PaymentCloseScheduler.java`
  - `@Service`，注入 `PaymentCloseService`
  - `@Scheduled(fixedDelayString = "${hill.payment.close-expired.fixed-delay-ms:60000}")`
  - 方法体：`paymentCloseService.closeExpiredPayments();`

- [X] 1.2 新建 `backend/src/main/java/com/hillcommerce/modules/order/scheduler/ShipmentScheduler.java`
  - `@Service`，注入 `ShipmentService`
  - `@Scheduled(fixedDelayString = "${hill.fulfillment.auto-complete.fixed-delay-ms:300000}")`
  - 方法体：`shipmentService.autoComplete();`

### Phase 2: 移动 AdminAnalyticsScheduler

- [X] 2.1 将 `AdminAnalyticsScheduler.java` 从 `modules/admin/service/` 移至 `modules/admin/scheduler/`
  - 更新 package 为 `com.hillcommerce.modules.admin.scheduler`
  - 新增 import：`com.hillcommerce.modules.admin.service.AnomalyDetectionService`
  - 文件内容其余不变

### Phase 3: 清理 Service 类

- [X] 3.1 从 `PaymentCloseService.java` 删除 `scheduledCloseExpiredPayments()` 方法和 `import org.springframework.scheduling.annotation.Scheduled`

- [X] 3.2 从 `ShipmentService.java` 删除 `scheduledAutoComplete()` 方法和 `import org.springframework.scheduling.annotation.Scheduled`

### Phase 4: 验证

- [X] 4.1 `mvn compile` 编译通过
- [ ] 4.2 运行 `PaymentIntegrationTest` 通过
- [ ] 4.3 运行 `FulfillmentIntegrationTest` 通过
