package com.hillcommerce.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户-角色多对多关联表实体。
 *
 * user_id + role_id 由数据库 uk_user_roles_user_role 唯一约束保证同一用户不会重复分配同一角色。
 */
@TableName("user_roles")
public class UserRoleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;

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

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
