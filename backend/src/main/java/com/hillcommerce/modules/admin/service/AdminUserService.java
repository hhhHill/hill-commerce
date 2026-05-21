package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.dto.AdminUserDtos.CreateMerchantRequest;
import static com.hillcommerce.modules.admin.dto.AdminUserDtos.MerchantUserResponse;
import static com.hillcommerce.modules.admin.dto.AdminUserDtos.UserActionResponse;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.user.entity.RoleEntity;
import com.hillcommerce.modules.user.entity.UserEntity;
import com.hillcommerce.modules.user.entity.UserRoleEntity;
import com.hillcommerce.modules.user.mapper.RoleMapper;
import com.hillcommerce.modules.user.mapper.UserMapper;
import com.hillcommerce.modules.user.mapper.UserRoleMapper;
import com.hillcommerce.modules.user.service.PasswordService;
import com.hillcommerce.modules.admin.mapper.ShopMapper;
import com.hillcommerce.modules.admin.entity.ShopEntity;

@Service
public class AdminUserService {

    private static final String ROLE_MERCHANT = "MERCHANT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordService passwordService;
    private final ShopMapper shopMapper;

    public AdminUserService(
        JdbcTemplate jdbcTemplate,
        UserMapper userMapper,
        UserRoleMapper userRoleMapper,
        RoleMapper roleMapper,
        PasswordService passwordService,
        ShopMapper shopMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordService = passwordService;
        this.shopMapper = shopMapper;
    }

    public List<MerchantUserResponse> listMerchantUsers() {
        return jdbcTemplate.query(
            """
            select u.id, u.email, u.nickname, u.status, u.created_at
            from users u
            join user_roles ur on ur.user_id = u.id
            join roles r on r.id = ur.role_id
            where r.code = ?
            order by u.created_at desc, u.id desc
            """,
            (rs, rowNum) -> new MerchantUserResponse(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("nickname"),
                STATUS_ACTIVE.equals(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime()),
            ROLE_MERCHANT);
    }

    @Transactional
    public MerchantUserResponse createMerchantUser(CreateMerchantRequest request) {
        String email = request.email().trim();
        if (existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setNickname(request.nickname().trim());
        user.setPasswordHash(passwordService.encode(request.password()));
        user.setStatus(STATUS_ACTIVE);
        userMapper.insert(user);

        RoleEntity merchantRole = requireRole(ROLE_MERCHANT);
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(merchantRole.getId());
        userRoleMapper.insert(userRole);

        // Auto-create shop for the new merchant
        ShopEntity shop = new ShopEntity();
        shop.setName(user.getNickname() + "的店铺");
        shop.setSlug(generateSlug(user.getEmail()));
        shop.setOwnerId(user.getId());
        shop.setStatus(STATUS_ACTIVE);
        shopMapper.insert(shop);

        return toMerchantUserResponse(requireMerchantUser(user.getId()));
    }

    private String generateSlug(String email) {
        String prefix = email.split("@")[0]
            .replaceAll("[^a-zA-Z0-9]", "-")
            .toLowerCase();
        String random = java.util.UUID.randomUUID().toString().substring(0, 6);
        return prefix + "-" + random;
    }

    @Transactional
    public UserActionResponse disableMerchantUser(Long targetUserId, Long operatorUserId) {
        if (targetUserId.equals(operatorUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF, "Admin cannot disable current user");
        }

        UserEntity merchantUser = requireMerchantUser(targetUserId);
        if (!STATUS_ACTIVE.equals(merchantUser.getStatus())) {
            return new UserActionResponse(merchantUser.getId(), false);
        }

        merchantUser.setStatus(STATUS_DISABLED);
        userMapper.updateById(merchantUser);

        // Sync shop status
        ShopEntity shop = shopMapper.findByOwnerId(merchantUser.getId());
        if (shop != null) {
            shop.setStatus(STATUS_DISABLED);
            shopMapper.updateById(shop);
        }

        return new UserActionResponse(merchantUser.getId(), false);
    }

    @Transactional
    public UserActionResponse enableMerchantUser(Long targetUserId) {
        UserEntity merchantUser = requireMerchantUser(targetUserId);
        if (STATUS_ACTIVE.equals(merchantUser.getStatus())) {
            return new UserActionResponse(merchantUser.getId(), true);
        }

        merchantUser.setStatus(STATUS_ACTIVE);
        userMapper.updateById(merchantUser);

        // Sync shop status
        ShopEntity shop = shopMapper.findByOwnerId(merchantUser.getId());
        if (shop != null) {
            shop.setStatus(STATUS_ACTIVE);
            shopMapper.updateById(shop);
        }

        return new UserActionResponse(merchantUser.getId(), true);
    }

    @Transactional
    public UserActionResponse resetPassword(Long targetUserId, String rawPassword) {
        UserEntity merchantUser = requireMerchantUser(targetUserId);
        merchantUser.setPasswordHash(passwordService.encode(rawPassword));
        userMapper.updateById(merchantUser);
        return new UserActionResponse(merchantUser.getId(), STATUS_ACTIVE.equals(merchantUser.getStatus()));
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
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + code);
        }
        return role;
    }

    private UserEntity requireMerchantUser(Long userId) {
        List<String> roles = userMapper.findRoleCodesByUserId(userId);
        if (!roles.contains(ROLE_MERCHANT) || roles.contains("ADMIN")) {
            throw new BusinessException(ErrorCode.MERCHANT_USER_NOT_FOUND, "Merchant user not found");
        }

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.MERCHANT_USER_NOT_FOUND, "Merchant user not found");
        }
        return user;
    }

    private MerchantUserResponse toMerchantUserResponse(UserEntity user) {
        LocalDateTime createdAt = user.getCreatedAt();
        if (createdAt == null) {
            Timestamp dbCreatedAt = jdbcTemplate.queryForObject(
                "select created_at from users where id = ?",
                Timestamp.class,
                user.getId());
            createdAt = dbCreatedAt.toLocalDateTime();
        }
        return new MerchantUserResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            STATUS_ACTIVE.equals(user.getStatus()),
            createdAt);
    }
}
