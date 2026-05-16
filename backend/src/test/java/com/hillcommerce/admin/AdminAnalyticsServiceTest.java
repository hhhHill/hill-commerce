package com.hillcommerce.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.hillcommerce.modules.admin.service.AnomalyDetectionService;
import com.hillcommerce.modules.admin.service.ProductRankingService;
import com.hillcommerce.modules.admin.service.SalesTrendService;
import com.hillcommerce.modules.admin.service.UserProfileService;
import com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.AnomalyItem;
import com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.ProductRankingResponse;
import com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.TrendResponse;
import com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.UserProfileDetail;

class AdminAnalyticsServiceTest {

    @Test
    void anomalyDetectionFlagsCurrentHourOutsideTwoStandardDeviations() throws Exception {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        LocalDateTime currentHour = LocalDateTime.of(2026, 5, 16, 10, 0);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                RowMapper<AnomalyDetectionService.HourlySnapshot> mapper = invocation.getArgument(1);
                ResultSet rs = Mockito.mock(ResultSet.class);
                when(rs.getTimestamp("snapshot_hour")).thenReturn(Timestamp.valueOf(currentHour));
                when(rs.getBigDecimal("total_amount")).thenReturn(new BigDecimal("200.00"));
                when(rs.getInt("order_count")).thenReturn(5);
                return List.of(mapper.mapRow(rs, 0));
            });
        when(jdbcTemplate.queryForList(anyString(), Mockito.eq(BigDecimal.class), any(), any(), any()))
            .thenReturn(List.of(new BigDecimal("100.00"), new BigDecimal("101.00"), new BigDecimal("99.00"), new BigDecimal("100.00")));

        AnomalyDetectionService service = new AnomalyDetectionService(jdbcTemplate);
        List<AnomalyItem> anomalies = service.detectLatest();

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().direction()).isEqualTo("high");
        assertThat(service.getStatus().hasAlert()).isTrue();
        service.acknowledge(anomalies.getFirst().id());
        assertThat(service.getStatus().hasAlert()).isFalse();
    }

    @Test
    void salesTrendCalculatesMovingAverageDirectionAndChangePercent() throws Exception {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 5, 3))))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                RowMapper<?> mapper = invocation.getArgument(1);
                return List.of(
                    mapTrend(mapper, "2026-05-01", "100.00", 0),
                    mapTrend(mapper, "2026-05-02", "200.00", 1),
                    mapTrend(mapper, "2026-05-03", "400.00", 2));
            });

        TrendResponse response = new SalesTrendService(jdbcTemplate)
            .getTrends("day", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3), 1L, false);

        assertThat(response.trendDirection()).isEqualTo("up");
        assertThat(response.changePercent()).isEqualByComparingTo("100.00");
        assertThat(response.points().get(2).movingAvg()).isEqualByComparingTo("233.33");
        assertThat(response.points().get(2).lastPeriodAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void productRankingNormalizesRangeAndCapsLimit() throws Exception {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(LocalDate.class), eq(50)))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                RowMapper<?> mapper = invocation.getArgument(1);
                ResultSet rs = Mockito.mock(ResultSet.class);
                when(rs.getLong("product_id")).thenReturn(10L);
                when(rs.getString("product_name")).thenReturn("Trail Jacket");
                when(rs.getLong("category_id")).thenReturn(3L);
                when(rs.getString("category_name")).thenReturn("Outerwear");
                when(rs.getInt("total_quantity")).thenReturn(12);
                when(rs.getBigDecimal("total_amount")).thenReturn(new BigDecimal("1299.00"));
                return List.of(mapper.mapRow(rs, 0));
            });

        ProductRankingResponse response = new ProductRankingService(jdbcTemplate).getRankings("invalid", 99, 1L, false);

        assertThat(response.range()).isEqualTo("today");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().totalQuantity()).isEqualTo(12);
    }

    @Test
    void userProfileBuildsPurchasingTierRegionAndPreferredCategories() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(eq("select id, email, nickname from users where id = ?"), any(RowMapper.class), eq(7L)))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                RowMapper<?> mapper = invocation.getArgument(1);
                ResultSet rs = Mockito.mock(ResultSet.class);
                when(rs.getLong("id")).thenReturn(7L);
                when(rs.getString("email")).thenReturn("buyer@example.com");
                when(rs.getString("nickname")).thenReturn("buyer");
                return mapper.mapRow(rs, 0);
            });
        when(jdbcTemplate.queryForObject(Mockito.contains("coalesce(sum(payable_amount), 0)"), eq(BigDecimal.class), eq(7L)))
            .thenReturn(new BigDecimal("5200.00"));
        when(jdbcTemplate.queryForObject(Mockito.contains("created_at >= date_sub"), eq(Integer.class), eq(7L)))
            .thenReturn(4);
        when(jdbcTemplate.query(Mockito.contains("from user_addresses"), any(RowMapper.class), eq(7L)))
            .thenReturn(List.of("上海/上海"));
        when(jdbcTemplate.query(Mockito.contains("from orders o"), any(RowMapper.class), eq(7L)))
            .thenReturn(List.of("户外", "鞋靴"));

        UserProfileDetail detail = new UserProfileService(jdbcTemplate).getUserProfile(7L);

        assertThat(detail.email()).isEqualTo("buyer@example.com");
        assertThat(detail.region()).isEqualTo("上海/上海");
        assertThat(detail.totalSpent()).isEqualByComparingTo("5200.00");
        assertThat(detail.purchasingPowerTier()).isEqualTo("high");
        assertThat(detail.preferredCategories()).containsExactly("户外", "鞋靴");
        assertThat(detail.orderCountLast90Days()).isEqualTo(4);
    }

    private Object mapTrend(RowMapper<?> mapper, String date, String amount, int rowNum) throws Exception {
        ResultSet rs = Mockito.mock(ResultSet.class);
        when(rs.getString("period_key")).thenReturn(date);
        when(rs.getBigDecimal("amount")).thenReturn(new BigDecimal(amount));
        return mapper.mapRow(rs, rowNum);
    }
}
