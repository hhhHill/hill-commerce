package com.hillcommerce.modules.order.service;

import static com.hillcommerce.modules.order.web.OrderCenterDtos.OrderListItemResponse;
import static com.hillcommerce.modules.order.web.OrderCenterDtos.OrderListQuery;
import static com.hillcommerce.modules.order.web.OrderCenterDtos.OrderListResponse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.modules.order.entity.OrderEntity;
import com.hillcommerce.modules.order.entity.OrderItemEntity;
import com.hillcommerce.modules.order.mapper.OrderItemMapper;
import com.hillcommerce.modules.order.mapper.OrderMapper;

@Service
public class OrderCenterService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final Set<String> ALLOWED_STATUSES = Set.of(
        OrderStatus.PENDING_PAYMENT.name(),
        OrderStatus.PAID.name(),
        OrderStatus.SHIPPED.name(),
        OrderStatus.COMPLETED.name(),
        OrderStatus.CANCELLED.name(),
        OrderStatus.CLOSED.name());

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderCenterService(OrderMapper orderMapper, OrderItemMapper orderItemMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    public OrderListResponse listOrders(Long userId, OrderListQuery query) {
        return listOrdersInternal(query, userId);
    }

    public OrderListResponse listAllOrders(OrderListQuery query) {
        return listOrdersInternal(query, null);
    }

    private OrderListResponse listOrdersInternal(OrderListQuery query, Long userId) {
        int page = normalizePage(query.page());
        int size = normalizeSize(query.size());
        String status = normalizeStatus(query.status());
        String orderNo = normalizeOrderNo(query.orderNo());

        List<OrderEntity> matchedOrders = orderMapper.selectList(
            new LambdaQueryWrapper<OrderEntity>()
                .eq(userId != null, OrderEntity::getUserId, userId)
                .eq(status != null, OrderEntity::getOrderStatus, status)
                .likeRight(orderNo != null, OrderEntity::getOrderNo, orderNo)
                .orderByDesc(OrderEntity::getCreatedAt)
                .orderByDesc(OrderEntity::getId));

        long total = matchedOrders.size();
        long totalPages = total == 0 ? 0 : (total + size - 1) / size;
        int offset = (page - 1) * size;
        List<OrderEntity> pageOrders = offset >= matchedOrders.size()
            ? List.of()
            : matchedOrders.subList(offset, Math.min(offset + size, matchedOrders.size()));

        Map<Long, SummarySnapshot> summaries = loadSummarySnapshots(pageOrders);
        List<OrderListItemResponse> items = pageOrders.stream()
            .map(order -> {
                SummarySnapshot summary = summaries.get(order.getId());
                return new OrderListItemResponse(
                    order.getId(),
                    order.getOrderNo(),
                    order.getUserId(),
                    order.getOrderStatus(),
                    order.getPayableAmount(),
                    order.getCreatedAt(),
                    summary == null ? null : summary.productName(),
                    summary == null ? 0 : summary.itemCount());
            })
            .toList();

        return new OrderListResponse(items, page, size, total, totalPages);
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new IllegalArgumentException("Page must be at least 1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be at least 1");
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be at most 50");
        }
        return size;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported order status");
        }
        return normalized;
    }

    private String normalizeOrderNo(String orderNo) {
        if (orderNo == null) {
            return null;
        }
        String normalized = orderNo.trim();
        if (normalized.isEmpty() || normalized.length() < 4) {
            return null;
        }
        return normalized;
    }

    private Map<Long, SummarySnapshot> loadSummarySnapshots(List<OrderEntity> pageOrders) {
        if (pageOrders.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> orderIds = pageOrders.stream()
            .map(OrderEntity::getId)
            .filter(Objects::nonNull)
            .toList();
        List<OrderItemEntity> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItemEntity>()
                .in(OrderItemEntity::getOrderId, orderIds)
                .orderByAsc(OrderItemEntity::getOrderId)
                .orderByAsc(OrderItemEntity::getId));

        Map<Long, SummarySnapshot> summaries = new LinkedHashMap<>();
        for (OrderItemEntity item : orderItems) {
            summaries.compute(item.getOrderId(), (orderId, existing) -> {
                if (existing == null) {
                    return new SummarySnapshot(item.getProductNameSnapshot(), 1);
                }
                return new SummarySnapshot(existing.productName(), existing.itemCount() + 1);
            });
        }
        return summaries;
    }

    private record SummarySnapshot(String productName, int itemCount) {
    }
}
