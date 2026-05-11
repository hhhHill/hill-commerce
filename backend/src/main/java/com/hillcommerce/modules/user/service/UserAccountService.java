package com.hillcommerce.modules.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.modules.user.entity.RoleEntity;
import com.hillcommerce.modules.user.entity.UserEntity;
import com.hillcommerce.modules.user.entity.UserRoleEntity;
import com.hillcommerce.modules.user.mapper.RoleMapper;
import com.hillcommerce.modules.user.mapper.UserMapper;
import com.hillcommerce.modules.user.mapper.UserRoleMapper;
import com.hillcommerce.modules.user.model.AuthUser;

/**
 * 用户账户核心服务，管理注册与凭据加载。
 *
 * 注册在一个 @Transactional 中完成：插入用户 → 查找 CUSTOMER 角色 → 分配 user_roles 关联。
 * loadByEmail 返回 null 而非抛异常，由 AppUserDetailsService 决定如何转换为 Spring Security 异常。
 */
@Service
public class UserAccountService {

    public static final String USER_STATUS_ACTIVE = "ACTIVE";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordService passwordService;

    public UserAccountService(
        UserMapper userMapper,
        RoleMapper roleMapper,
        UserRoleMapper userRoleMapper,
        PasswordService passwordService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordService = passwordService;
    }

    /**
     * 注册新账户，默认分配 CUSTOMER 角色。
     * DuplicateKeyException 来自数据库 uk_users_email 唯一约束，转换为业务异常。
     * customerRole 不存在时抛 IllegalStateException——说明 V2 迁移脚本未执行。
     */
    @Transactional
    public AuthUser register(String email, String rawPassword, String nickname) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordService.encode(rawPassword));
        user.setNickname(nickname);
        user.setStatus(USER_STATUS_ACTIVE);
        try {
            userMapper.insert(user);
        }
        catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("Email already exists");
        }

        RoleEntity customerRole = roleMapper.selectOne(
            new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, ROLE_CUSTOMER));
        if (customerRole == null) {
            throw new IllegalStateException("Default customer role not found");
        }

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(customerRole.getId());
        userRoleMapper.insert(userRole);

        return loadByEmail(email);
    }

    /**
     * 按邮箱加载用户及角色列表，返回 null 表示用户不存在。
     * 返回的 AuthUser 包含 passwordHash，仅供认证阶段校验；认证成功后不得直接写入 HTTP Session。
     */
    public AuthUser loadByEmail(String email) {
        UserEntity user = userMapper.selectOne(
            new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, email));
        if (user == null) {
            return null;
        }

        List<String> roleCodes = userMapper.findRoleCodesByUserId(user.getId());
        return new AuthUser(
            user.getId(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getNickname(),
            user.getStatus(),
            roleCodes);
    }

    /** 成功登录后记录最近一次登录时间。 */
    public void recordSuccessfulLogin(Long userId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
