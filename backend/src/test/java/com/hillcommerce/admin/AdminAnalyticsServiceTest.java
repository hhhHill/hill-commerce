package com.hillcommerce.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AnomalyListResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.ProductRankingResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.TrendResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.UserProfileDetail;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.hillcommerce.modules.admin.service.AnomalyDetectionService;
import com.hillcommerce.modules.admin.service.ProductRankingService;
import com.hillcommerce.modules.admin.service.SalesTrendService;
import com.hillcommerce.modules.admin.service.UserProfileService;

class AdminAnalyticsServiceTest {

    @Test
    void anomalyDetectionFlagsCurrentHourOutsideTwoStandardDeviations() throws Exception {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        LocalDateTime currentHour = LocalDateTime.of(2026, 5, 16, 10, 0);

        // Mock: 取最近一小时快照（shopId=0 → query(sql, mapper) 2 参数版本）
        when(jdbcTemplate.query(contains("hourly_sales_snapshot"), any(RowMapper.class)))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                RowMapper<AnomalyDetectionService.HourlySnapshot> mapper = invocation.getArgument(1);
                ResultSet rs = Mockito.mock(ResultSet.class);
                when(rs.getTimestamp("snapshot_hour")).thenReturn(Timestamp.valueOf(currentHour));
                when(rs.getBigDecimal("total_amount")).thenReturn(new BigDecimal("200.00"));
                when(rs.getInt("order_count")).thenReturn(5);
                when(rs.getLong("shop_id")).thenReturn(0L);
                return List.of(mapper.mapRow(rs, 0));
            });

        // Mock: baseline 查询（shopId=0 → query(sql, mapper, arg, arg, arg) 5 参数版本）
        when(jdbcTemplate.query(contains("hour(snapshot_hour)"), any(RowMapper.class), any(), any(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                RowMapper<BigDecimal> mapper = invocation.getArgument(1);
                List<BigDecimal> values = List.of(new BigDecimal("100.00"), new BigDecimal("101.00"),
                    new BigDecimal("99.00"), new BigDecimal("100.00"));
                List<BigDecimal> result = new ArrayList<>();
                for (BigDecimal v : values) {
                    ResultSet rs = Mockito.mock(ResultSet.class);
                    when(rs.getBigDecimal("total_amount")).thenReturn(v);
                    result.add(mapper.mapRow(rs, 0));
                }
                return result;
            });

        AnomalyDetectionService service = new AnomalyDetectionService(jdbcTemplate);
        service.detectLatest(0);

        // 验证检测到异常 → update 被调用（INSERT INTO anomaly_alerts）
        verify(jdbcTemplate, atLeastOnce()).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any());

        // Mock: anomaly_alerts 列表查询（currentAnomalies）
        when(jdbcTemplate.query(contains("anomaly_alerts"), any(RowMapper.class), anyLong(), anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                RowMapper<AdminAnalyticsDtos.AnomalyItem> mapper = invocation.getArgument(1);
                ResultSet rs = Mockito.mock(ResultSet.class);
                when(rs.getLong("id")).thenReturn(1L);
                when(rs.getString("snapshot_hour")).thenReturn(currentHour.toString());
                when(rs.getBigDecimal("total_amount")).thenReturn(new BigDecimal("200.00"));
                when(rs.getBigDecimal("baseline_mean")).thenReturn(new BigDecimal("100.00"));
                when(rs.getBigDecimal("baseline_std")).thenReturn(new BigDecimal("0.71"));
                when(rs.getString("direction")).thenReturn("high");
                when(rs.getBigDecimal("deviation_pct")).thenReturn(new BigDecimal("100.00"));
                return List.of(mapper.mapRow(rs, 0));
            });
        when(jdbcTemplate.queryForObject(contains("count(*) from anomaly_alerts"), eq(Long.class), anyLong()))
            .thenReturn(1L);

        AnomalyListResponse response = service.currentAnomalies(0, 1, 10);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().direction()).isEqualTo("high");
        assertThat(response.hasAlert()).isTrue();

        // getStatus
        assertThat(service.getStatus(0).hasAlert()).isTrue();
        assertThat(service.getStatus(0).count()).isEqualTo(1);

        // acknowledge → 重新 mock count 为 0 验证已清空
        service.acknowledge(1, "testOperator");
        when(jdbcTemplate.queryForObject(contains("count(*) from anomaly_alerts"), eq(Long.class), anyLong()))
            .thenReturn(0L);
        assertThat(service.getStatus(0).hasAlert()).isFalse();
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
            .getTrends("day", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3), null);

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

        ProductRankingResponse response = new ProductRankingService(jdbcTemplate).getRankings("invalid", 99, null);

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
