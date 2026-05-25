package com.hillcommerce.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.hillcommerce.framework.analytics.CacheEvictionService;
import com.hillcommerce.modules.admin.scheduler.AdminAnalyticsScheduler;
import com.hillcommerce.modules.admin.service.AnomalyDetectionService;

class AdminAnalyticsSchedulerTest {

    private JdbcTemplate jdbcTemplate;
    private AnomalyDetectionService anomalyDetectionService;
    private CacheEvictionService cacheEvictionService;
    private AdminAnalyticsScheduler scheduler;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        anomalyDetectionService = mock(AnomalyDetectionService.class);
        cacheEvictionService = mock(CacheEvictionService.class);
        scheduler = new AdminAnalyticsScheduler(
            jdbcTemplate, anomalyDetectionService, cacheEvictionService);
    }

    @Test
    void snapshotHourlySalesShouldInsertBothMerchantAndPlatformRows() {
        when(jdbcTemplate.update(any(String.class), any(Object[].class))).thenReturn(3);

        scheduler.snapshotHourlySales();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("union all"), "SQL should contain UNION ALL");
        assertTrue(sql.contains("o.shop_id is not null"), "Merchant part should filter null shop_id");
    }

    @Test
    void computeDailySummaryShouldEvictCache() {
        when(jdbcTemplate.update(any(String.class), any(Object[].class))).thenReturn(3);

        scheduler.computeDailySummary();

        verify(cacheEvictionService).evictAnalyticsCaches();
    }

    @Test
    void computeDailySummaryShouldUseRangeFilter() {
        when(jdbcTemplate.update(any(String.class), any(Object[].class))).thenReturn(3);

        scheduler.computeDailySummary();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.times(2))
            .update(sqlCaptor.capture(), any(Object[].class));
        for (String sql : sqlCaptor.getAllValues()) {
            assertTrue(sql.contains("created_at >="),
                "Each SQL should use range filter not date() function: " + sql);
        }
    }

    @Test
    void detectAnomaliesShouldCheckPlatformAndAllActiveShops() {
        when(jdbcTemplate.queryForList(
            eq("select id from shops where status = 'ACTIVE'"), eq(Long.class)))
            .thenReturn(List.of(1L, 2L, 3L));

        scheduler.detectAnomalies();

        verify(anomalyDetectionService).detectLatest(0L);
        verify(anomalyDetectionService).detectLatest(1L);
        verify(anomalyDetectionService).detectLatest(2L);
        verify(anomalyDetectionService).detectLatest(3L);
    }
}
