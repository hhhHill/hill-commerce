package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.AggregateProfileResponse;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.CategoryPreference;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.PurchasingPowerTier;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.RegionDistribution;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.UserProfileDetail;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.UserProfileSummary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final JdbcTemplate jdbcTemplate;

    public UserProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AggregateProfileResponse getAggregateProfiles() {
        long totalUsers = countTotalUsers();
        long repeatPurchaseUsers = countRepeatPurchaseUsers();
        BigDecimal repeatRate = totalUsers == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(repeatPurchaseUsers).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP);
        return new AggregateProfileResponse(
            regionDistribution(),
            purchasingPowerTiers(),
            categoryPreferences(),
            totalUsers,
            repeatPurchaseUsers,
            repeatRate);
    }

    public List<UserProfileSummary> searchUsers(String keyword) {
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return jdbcTemplate.query(
            """
            select id, email, nickname
            from users
            where email like ? or nickname like ?
            order by id desc
            limit 20
            """,
            (rs, rowNum) -> new UserProfileSummary(rs.getLong("id"), rs.getString("email"), rs.getString("nickname")),
            pattern,
            pattern);
    }

    public UserProfileDetail getUserProfile(Long userId) {
        UserProfileSummary user = jdbcTemplate.queryForObject(
            "select id, email, nickname from users where id = ?",
            (rs, rowNum) -> new UserProfileSummary(rs.getLong("id"), rs.getString("email"), rs.getString("nickname")),
            userId);
        BigDecimal totalSpent = valueOrZero(jdbcTemplate.queryForObject(
            """
            select coalesce(sum(payable_amount), 0)
            from orders
            where user_id = ? and order_status in ('PAID', 'SHIPPED', 'COMPLETED')
            """,
            BigDecimal.class,
            userId));
        Integer last90 = jdbcTemplate.queryForObject(
            """
            select count(*)
            from orders
            where user_id = ? and created_at >= date_sub(now(3), interval 90 day)
            """,
            Integer.class,
            userId);
        return new UserProfileDetail(
            user.userId(),
            user.email(),
            user.nickname(),
            regionForUser(userId),
            totalSpent,
            purchasingPowerTier(totalSpent),
            preferredCategories(userId),
            last90 == null ? 0 : last90);
    }

    private List<RegionDistribution> regionDistribution() {
        return jdbcTemplate.query(
            """
            select region, count(*) as user_count
            from (
              select u.id, coalesce(max(concat(ua.province, '/', ua.city)), max(concat(o.address_snapshot_province, '/', o.address_snapshot_city)), '未知') as region
              from users u
              left join user_addresses ua on ua.user_id = u.id and ua.is_default = 1
              left join orders o on o.user_id = u.id
              group by u.id
            ) t
            group by region
            order by user_count desc
            limit 20
            """,
            (rs, rowNum) -> new RegionDistribution(rs.getString("region"), rs.getLong("user_count")));
    }

    private List<PurchasingPowerTier> purchasingPowerTiers() {
        return jdbcTemplate.query(
            """
            select tier, count(*) as user_count, coalesce(sum(total_spent), 0) as total_amount
            from (
              select user_id, total_spent,
                     case
                       when total_spent < 500 then 'low'
                       when total_spent < 5000 then 'mid'
                       else 'high'
                     end as tier
              from (
                select u.id as user_id, coalesce(sum(o.payable_amount), 0) as total_spent
                from users u
                left join orders o on o.user_id = u.id and o.order_status in ('PAID', 'SHIPPED', 'COMPLETED')
                group by u.id
              ) spending
            ) tiered
            group by tier
            order by field(tier, 'low', 'mid', 'high')
            """,
            (rs, rowNum) -> new PurchasingPowerTier(rs.getString("tier"), rs.getLong("user_count"), rs.getBigDecimal("total_amount")));
    }

    private List<CategoryPreference> categoryPreferences() {
        return jdbcTemplate.query(
            """
            select p.category_id, pc.name as category_name, count(distinct o.id) as order_count
            from orders o
            join order_items oi on oi.order_id = o.id
            join products p on p.id = oi.product_id
            left join product_categories pc on pc.id = p.category_id
            where o.order_status in ('PAID', 'SHIPPED', 'COMPLETED')
            group by p.category_id, pc.name
            order by order_count desc
            limit 10
            """,
            this::mapCategoryPreference);
    }

    private List<String> preferredCategories(Long userId) {
        return jdbcTemplate.query(
            """
            select pc.name as category_name
            from orders o
            join order_items oi on oi.order_id = o.id
            join products p on p.id = oi.product_id
            left join product_categories pc on pc.id = p.category_id
            where o.user_id = ?
              and o.order_status in ('PAID', 'SHIPPED', 'COMPLETED')
            group by pc.id, pc.name
            order by count(*) desc
            limit 3
            """,
            (rs, rowNum) -> rs.getString("category_name"),
            userId);
    }

    private CategoryPreference mapCategoryPreference(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryPreference(rs.getLong("category_id"), rs.getString("category_name"), rs.getLong("order_count"));
    }

    private String regionForUser(Long userId) {
        List<String> regions = jdbcTemplate.query(
            """
            select concat(province, '/', city) as region
            from user_addresses
            where user_id = ?
            order by is_default desc, id desc
            limit 1
            """,
            (rs, rowNum) -> rs.getString("region"),
            userId);
        if (!regions.isEmpty()) {
            return regions.getFirst();
        }
        List<String> orderRegions = jdbcTemplate.query(
            """
            select concat(address_snapshot_province, '/', address_snapshot_city) as region
            from orders
            where user_id = ?
            order by created_at desc
            limit 1
            """,
            (rs, rowNum) -> rs.getString("region"),
            userId);
        return orderRegions.isEmpty() ? "未知" : orderRegions.getFirst();
    }

    private long countTotalUsers() {
        Long count = jdbcTemplate.queryForObject("select count(*) from users", Long.class);
        return count == null ? 0 : count;
    }

    private long countRepeatPurchaseUsers() {
        Long count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from (
              select user_id
              from orders
              where order_status in ('PAID', 'SHIPPED', 'COMPLETED')
              group by user_id
              having count(*) >= 2
            ) repeat_users
            """,
            Long.class);
        return count == null ? 0 : count;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String purchasingPowerTier(BigDecimal totalSpent) {
        if (totalSpent.compareTo(BigDecimal.valueOf(500)) < 0) {
            return "low";
        }
        if (totalSpent.compareTo(BigDecimal.valueOf(5000)) < 0) {
            return "mid";
        }
        return "high";
    }
}
