package com.hillcommerce.modules.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("order_items")
public class OrderItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private Long skuId;
    private String productNameSnapshot;
    private String skuCodeSnapshot;
    private String skuAttrTextSnapshot;
    private String productImageSnapshot;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotalAmount;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public void setProductNameSnapshot(String productNameSnapshot) {
        this.productNameSnapshot = productNameSnapshot;
    }

    public String getSkuCodeSnapshot() {
        return skuCodeSnapshot;
    }

    public void setSkuCodeSnapshot(String skuCodeSnapshot) {
        this.skuCodeSnapshot = skuCodeSnapshot;
    }

    public String getSkuAttrTextSnapshot() {
        return skuAttrTextSnapshot;
    }

    public void setSkuAttrTextSnapshot(String skuAttrTextSnapshot) {
        this.skuAttrTextSnapshot = skuAttrTextSnapshot;
    }

    public String getProductImageSnapshot() {
        return productImageSnapshot;
    }

    public void setProductImageSnapshot(String productImageSnapshot) {
        this.productImageSnapshot = productImageSnapshot;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
