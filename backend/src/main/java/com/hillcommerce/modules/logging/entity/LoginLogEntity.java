package com.hillcommerce.modules.logging.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("login_logs")
public class LoginLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String emailSnapshot;
    private String roleSnapshot;
    private String loginResult;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loginAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmailSnapshot() {
        return emailSnapshot;
    }

    public void setEmailSnapshot(String emailSnapshot) {
        this.emailSnapshot = emailSnapshot;
    }

    public String getRoleSnapshot() {
        return roleSnapshot;
    }

    public void setRoleSnapshot(String roleSnapshot) {
        this.roleSnapshot = roleSnapshot;
    }

    public String getLoginResult() {
        return loginResult;
    }

    public void setLoginResult(String loginResult) {
        this.loginResult = loginResult;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(LocalDateTime loginAt) {
        this.loginAt = loginAt;
    }
}
