package com.hillcommerce.modules.user.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 统一账号体系下的用户表实体。
 *
 * 所有角色（CUSTOMER / SALES / ADMIN）共用此表，通过 user_roles 关联区分。
 * email 由数据库 uk_users_email 唯一约束保证不重复。
 * status 当前约定值为 ACTIVE，由 AppUserPrincipal#isEnabled 驱动 Spring Security 账户启用判断。
 * lastLoginAt 由 AuthController#login 在登录成功后更新。
 */
@TableName("users")
public class UserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 登录邮箱，数据库唯一约束保证不可重复，首版不支持修改邮箱 */
    private String email;
    /** 运行时和种子数据统一使用 BCrypt 哈希 */
    private String passwordHash;
    private String nickname;
    /** 账户状态（当前仅 ACTIVE），驱动 isEnabled 判断 */
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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
