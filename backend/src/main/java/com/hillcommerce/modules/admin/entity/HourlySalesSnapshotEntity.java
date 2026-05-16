package com.hillcommerce.modules.admin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("hourly_sales_snapshot")
public class HourlySalesSnapshotEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDateTime snapshotHour;
    private Integer orderCount;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getSnapshotHour() { return snapshotHour; }
    public void setSnapshotHour(LocalDateTime snapshotHour) { this.snapshotHour = snapshotHour; }
    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
