package com.hillcommerce.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

class SchedulerSeparationTest {

    @Test
    void paymentCloseSchedulerOwnsScheduledTriggerAndDelegatesToService() throws Exception {
        Class<?> schedulerClass = Class.forName("com.hillcommerce.modules.payment.scheduler.PaymentCloseScheduler");
        Class<?> serviceClass = Class.forName("com.hillcommerce.modules.payment.service.PaymentCloseService");

        assertThat(schedulerClass.isAnnotationPresent(Service.class)).isTrue();
        assertScheduledMethod(
            schedulerClass,
            "scheduledCloseExpiredPayments",
            "${hill.payment.close-expired.fixed-delay-ms:60000}");

        Object service = mock(serviceClass);
        Object scheduler = schedulerClass.getConstructor(serviceClass).newInstance(service);
        schedulerClass.getMethod("scheduledCloseExpiredPayments").invoke(scheduler);

        Object verifiedService = verify(service);
        serviceClass.getMethod("closeExpiredPayments").invoke(verifiedService);
    }

    @Test
    void shipmentSchedulerOwnsScheduledTriggerAndDelegatesToService() throws Exception {
        Class<?> schedulerClass = Class.forName("com.hillcommerce.modules.order.scheduler.ShipmentScheduler");
        Class<?> serviceClass = Class.forName("com.hillcommerce.modules.order.service.ShipmentService");

        assertThat(schedulerClass.isAnnotationPresent(Service.class)).isTrue();
        assertScheduledMethod(
            schedulerClass,
            "scheduledAutoComplete",
            "${hill.fulfillment.auto-complete.fixed-delay-ms:300000}");

        Object service = mock(serviceClass);
        Object scheduler = schedulerClass.getConstructor(serviceClass).newInstance(service);
        schedulerClass.getMethod("scheduledAutoComplete").invoke(scheduler);

        Object verifiedService = verify(service);
        serviceClass.getMethod("autoComplete", Long.class).invoke(verifiedService, (Long) null);
    }

    @Test
    void adminAnalyticsSchedulerLivesInSchedulerPackageWithExistingScheduledMethods() throws Exception {
        Class<?> schedulerClass = Class.forName("com.hillcommerce.modules.admin.scheduler.AdminAnalyticsScheduler");

        assertThat(schedulerClass.isAnnotationPresent(Service.class)).isTrue();
        assertThat(schedulerClass.getMethod("snapshotHourlySales").isAnnotationPresent(Scheduled.class)).isTrue();
        assertThat(schedulerClass.getMethod("snapshotHourlySales").isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(schedulerClass.getMethod("computeDailySummary").isAnnotationPresent(Scheduled.class)).isTrue();
        assertThat(schedulerClass.getMethod("computeDailySummary").isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(schedulerClass.getMethod("detectAnomalies").isAnnotationPresent(Scheduled.class)).isTrue();
    }

    @Test
    void businessServicesDoNotOwnScheduledMethods() throws Exception {
        assertNoScheduledMethods("com.hillcommerce.modules.payment.service.PaymentCloseService");
        assertNoScheduledMethods("com.hillcommerce.modules.order.service.ShipmentService");
    }

    private void assertScheduledMethod(Class<?> schedulerClass, String methodName, String fixedDelayString)
        throws NoSuchMethodException {
        Method method = schedulerClass.getMethod(methodName);
        assertThat(method.isAnnotationPresent(Scheduled.class)).isTrue();
        assertThat(method.getAnnotation(Scheduled.class).fixedDelayString()).isEqualTo(fixedDelayString);
        assertThat(method.isAnnotationPresent(Transactional.class)).isFalse();
    }

    private void assertNoScheduledMethods(String className) throws ClassNotFoundException {
        Class<?> serviceClass = Class.forName(className);
        assertThat(Arrays.stream(serviceClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Scheduled.class))
            .map(Method::getName))
            .isEmpty();
    }
}
