package com.hillcommerce.modules.user.service;

import static com.hillcommerce.modules.user.web.UserAddressDtos.UserAddressRequest;
import static com.hillcommerce.modules.user.web.UserAddressDtos.UserAddressResponse;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hillcommerce.modules.user.entity.UserAddressEntity;
import com.hillcommerce.modules.user.mapper.UserAddressMapper;

@Service
public class UserAddressService {

    private static final Logger log = LoggerFactory.getLogger(UserAddressService.class);

    private final UserAddressMapper userAddressMapper;

    public UserAddressService(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    public List<UserAddressResponse> listAddresses(Long userId) {
        return listAddressEntities(userId).stream().map(this::toResponse).toList();
    }

    public UserAddressResponse getDefaultAddress(Long userId) {
        return findDefaultAddressEntity(userId) == null ? null : toResponse(findDefaultAddressEntity(userId));
    }

    @Transactional
    public UserAddressResponse createAddress(Long userId, UserAddressRequest request) {
        boolean shouldBeDefault = listAddressEntities(userId).isEmpty();

        UserAddressEntity entity = new UserAddressEntity();
        entity.setUserId(userId);
        applyRequest(entity, request);
        entity.setIsDefault(shouldBeDefault);
        userAddressMapper.insert(entity);
        log.info("Address created: userId={}, addressId={}, receiverName={}, isDefault={}",
            userId, entity.getId(), entity.getReceiverName(), shouldBeDefault);
        return toResponse(entity);
    }

    @Transactional
    public UserAddressResponse updateAddress(Long userId, Long addressId, UserAddressRequest request) {
        UserAddressEntity entity = requireOwnedAddress(userId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(entity.getIsDefault());
        applyRequest(entity, request);
        userAddressMapper.updateById(entity);
        log.info("Address updated: userId={}, addressId={}, receiverName={}, isDefaultPreserved={}",
            userId, addressId, entity.getReceiverName(), wasDefault);
        return toResponse(entity);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddressEntity entity = requireOwnedAddress(userId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(entity.getIsDefault());
        userAddressMapper.deleteById(addressId);

        if (wasDefault) {
            List<UserAddressEntity> remaining = listAddressEntities(userId);
            if (!remaining.isEmpty()) {
                promoteDefault(userId, remaining.getFirst().getId());
            }
        }
    }

    @Transactional
    public UserAddressResponse setDefaultAddress(Long userId, Long addressId) {
        UserAddressEntity entity = requireOwnedAddress(userId, addressId);
        promoteDefault(userId, entity.getId());
        entity.setIsDefault(true);
        return toResponse(entity);
    }

    private void promoteDefault(Long userId, Long addressId) {
        userAddressMapper.update(
            null,
            new LambdaUpdateWrapper<UserAddressEntity>()
                .eq(UserAddressEntity::getUserId, userId)
                .set(UserAddressEntity::getIsDefault, false));

        userAddressMapper.update(
            null,
            new LambdaUpdateWrapper<UserAddressEntity>()
                .eq(UserAddressEntity::getUserId, userId)
                .eq(UserAddressEntity::getId, addressId)
                .set(UserAddressEntity::getIsDefault, true));
    }

    private List<UserAddressEntity> listAddressEntities(Long userId) {
        return userAddressMapper.selectList(
            new LambdaQueryWrapper<UserAddressEntity>()
                .eq(UserAddressEntity::getUserId, userId)
                .orderByDesc(UserAddressEntity::getIsDefault, UserAddressEntity::getUpdatedAt, UserAddressEntity::getId));
    }

    private UserAddressEntity findDefaultAddressEntity(Long userId) {
        return userAddressMapper.selectOne(
            new LambdaQueryWrapper<UserAddressEntity>()
                .eq(UserAddressEntity::getUserId, userId)
                .eq(UserAddressEntity::getIsDefault, true));
    }

    private UserAddressEntity requireOwnedAddress(Long userId, Long addressId) {
        UserAddressEntity entity = userAddressMapper.selectById(addressId);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found");
        }
        return entity;
    }

    private void applyRequest(UserAddressEntity entity, UserAddressRequest request) {
        entity.setReceiverName(request.receiverName().trim());
        entity.setReceiverPhone(request.receiverPhone().trim());
        entity.setProvince(request.province().trim());
        entity.setCity(request.city().trim());
        entity.setDistrict(request.district().trim());
        entity.setDetailAddress(request.detailAddress().trim());
        entity.setPostalCode(request.postalCode() == null ? null : request.postalCode().trim());
    }

    private UserAddressResponse toResponse(UserAddressEntity entity) {
        return new UserAddressResponse(
            entity.getId(),
            entity.getReceiverName(),
            entity.getReceiverPhone(),
            entity.getProvince(),
            entity.getCity(),
            entity.getDistrict(),
            entity.getDetailAddress(),
            entity.getPostalCode(),
            Boolean.TRUE.equals(entity.getIsDefault()));
    }
}
