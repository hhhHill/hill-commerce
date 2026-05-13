package com.hillcommerce.modules.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("orders")
public class OrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private String orderStatus;
    private BigDecimal totalAmount;
    private BigDecimal payableAmount;
    private LocalDateTime paymentDeadlineAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String addressSnapshotName;
    private String addressSnapshotPhone;
    private String addressSnapshotProvince;
    private String addressSnapshotCity;
    private String addressSnapshotDistrict;
    private String addressSnapshotDetail;
    private String addressSnapshotPostalCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }

    public LocalDateTime getPaymentDeadlineAt() {
        return paymentDeadlineAt;
    }

    public void setPaymentDeadlineAt(LocalDateTime paymentDeadlineAt) {
        this.paymentDeadlineAt = paymentDeadlineAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getAddressSnapshotName() {
        return addressSnapshotName;
    }

    public void setAddressSnapshotName(String addressSnapshotName) {
        this.addressSnapshotName = addressSnapshotName;
    }

    public String getAddressSnapshotPhone() {
        return addressSnapshotPhone;
    }

    public void setAddressSnapshotPhone(String addressSnapshotPhone) {
        this.addressSnapshotPhone = addressSnapshotPhone;
    }

    public String getAddressSnapshotProvince() {
        return addressSnapshotProvince;
    }

    public void setAddressSnapshotProvince(String addressSnapshotProvince) {
        this.addressSnapshotProvince = addressSnapshotProvince;
    }

    public String getAddressSnapshotCity() {
        return addressSnapshotCity;
    }

    public void setAddressSnapshotCity(String addressSnapshotCity) {
        this.addressSnapshotCity = addressSnapshotCity;
    }

    public String getAddressSnapshotDistrict() {
        return addressSnapshotDistrict;
    }

    public void setAddressSnapshotDistrict(String addressSnapshotDistrict) {
        this.addressSnapshotDistrict = addressSnapshotDistrict;
    }

    public String getAddressSnapshotDetail() {
        return addressSnapshotDetail;
    }

    public void setAddressSnapshotDetail(String addressSnapshotDetail) {
        this.addressSnapshotDetail = addressSnapshotDetail;
    }

    public String getAddressSnapshotPostalCode() {
        return addressSnapshotPostalCode;
    }

    public void setAddressSnapshotPostalCode(String addressSnapshotPostalCode) {
        this.addressSnapshotPostalCode = addressSnapshotPostalCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
