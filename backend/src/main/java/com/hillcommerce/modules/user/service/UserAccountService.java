package com.hillcommerce.modules.user.service;

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
}
