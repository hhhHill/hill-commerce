package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.web.AdminUserDtos.CreateSalesRequest;
import static com.hillcommerce.modules.admin.web.AdminUserDtos.SalesUserResponse;
import static com.hillcommerce.modules.admin.web.AdminUserDtos.UserActionResponse;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.modules.user.entity.RoleEntity;
import com.hillcommerce.modules.user.entity.UserEntity;
import com.hillcommerce.modules.user.entity.UserRoleEntity;
import com.hillcommerce.modules.user.mapper.RoleMapper;
import com.hillcommerce.modules.user.mapper.UserMapper;
import com.hillcommerce.modules.user.mapper.UserRoleMapper;
import com.hillcommerce.modules.user.service.PasswordService;

@Service
public class AdminUserService {

    private static final String ROLE_SALES = "SALES";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordService passwordService;

    public AdminUserService(
        JdbcTemplate jdbcTemplate,
        UserMapper userMapper,
        UserRoleMapper userRoleMapper,
        RoleMapper roleMapper,
        PasswordService passwordService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordService = passwordService;
    }

    public List<SalesUserResponse> listSalesUsers() {
        return jdbcTemplate.query(
            """
            select u.id, u.email, u.nickname, u.status, u.created_at
            from users u
            join user_roles ur on ur.user_id = u.id
            join roles r on r.id = ur.role_id
            where r.code = ?
            order by u.created_at desc, u.id desc
            """,
            (rs, rowNum) -> new SalesUserResponse(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("nickname"),
                STATUS_ACTIVE.equals(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime()),
            ROLE_SALES);
    }

    @Transactional
    public SalesUserResponse createSalesUser(CreateSalesRequest request) {
        String email = request.email().trim();
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setNickname(request.nickname().trim());
        user.setPasswordHash(passwordService.encode(request.password()));
        user.setStatus(STATUS_ACTIVE);
        userMapper.insert(user);

        RoleEntity salesRole = requireRole(ROLE_SALES);
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(salesRole.getId());
        userRoleMapper.insert(userRole);

        return toSalesUserResponse(requireSalesUser(user.getId()));
    }

    @Transactional
    public UserActionResponse disableSalesUser(Long targetUserId, Long operatorUserId) {
        if (targetUserId.equals(operatorUserId)) {
            throw new IllegalArgumentException("Admin cannot disable current user");
        }

        UserEntity salesUser = requireSalesUser(targetUserId);
        if (!STATUS_ACTIVE.equals(salesUser.getStatus())) {
            return new UserActionResponse(salesUser.getId(), false);
        }

        salesUser.setStatus(STATUS_DISABLED);
        userMapper.updateById(salesUser);
        return new UserActionResponse(salesUser.getId(), false);
    }

    @Transactional
    public UserActionResponse enableSalesUser(Long targetUserId) {
        UserEntity salesUser = requireSalesUser(targetUserId);
        if (STATUS_ACTIVE.equals(salesUser.getStatus())) {
            return new UserActionResponse(salesUser.getId(), true);
        }

        salesUser.setStatus(STATUS_ACTIVE);
        userMapper.updateById(salesUser);
        return new UserActionResponse(salesUser.getId(), true);
    }

    @Transactional
    public UserActionResponse resetPassword(Long targetUserId, String rawPassword) {
        UserEntity salesUser = requireSalesUser(targetUserId);
        salesUser.setPasswordHash(passwordService.encode(rawPassword));
        userMapper.updateById(salesUser);
        return new UserActionResponse(salesUser.getId(), STATUS_ACTIVE.equals(salesUser.getStatus()));
    }

    private boolean existsByEmail(String email) {
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, email));
        return count != null && count > 0;
    }

    private RoleEntity requireRole(String code) {
        RoleEntity role = roleMapper.selectOne(
            new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getCode, code));
        if (role == null) {
            throw new IllegalStateException("Role not found: " + code);
        }
        return role;
    }

    private UserEntity requireSalesUser(Long userId) {
        List<String> roles = userMapper.findRoleCodesByUserId(userId);
        if (!roles.contains(ROLE_SALES) || roles.contains("ADMIN")) {
            throw new IllegalArgumentException("Sales user not found");
        }

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Sales user not found");
        }
        return user;
    }

    private SalesUserResponse toSalesUserResponse(UserEntity user) {
        LocalDateTime createdAt = user.getCreatedAt();
        if (createdAt == null) {
            Timestamp dbCreatedAt = jdbcTemplate.queryForObject(
                "select created_at from users where id = ?",
                Timestamp.class,
                user.getId());
            createdAt = dbCreatedAt.toLocalDateTime();
        }
        return new SalesUserResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            STATUS_ACTIVE.equals(user.getStatus()),
            createdAt);
    }
}
