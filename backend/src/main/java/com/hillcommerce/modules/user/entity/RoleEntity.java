package com.hillcommerce.modules.user.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 角色字典表实体。
 *
 * code 用于 Spring Security 的 hasRole/hasAnyRole 校验（自动拼接 ROLE_ 前缀），
 * 由 V2 迁移脚本种子 CUSTOMER / SALES / ADMIN 三个固定角色。
 */
@TableName("roles")
public class RoleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 角色编码，与 Spring Security GrantedAuthority 中 ROLE_xxx 的 xxx 部分对应 */
    private String code;
    private String name;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
