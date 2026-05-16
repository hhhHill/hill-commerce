package com.hillcommerce.modules.logging.service;

import org.springframework.stereotype.Service;

import com.hillcommerce.modules.logging.entity.LoginLogEntity;
import com.hillcommerce.modules.logging.entity.OperationLogEntity;
import com.hillcommerce.modules.logging.entity.ProductViewLogEntity;
import com.hillcommerce.modules.logging.mapper.LoginLogMapper;
import com.hillcommerce.modules.logging.mapper.OperationLogMapper;
import com.hillcommerce.modules.logging.mapper.ProductViewLogMapper;

@Service
public class LoggingService {

    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final ProductViewLogMapper productViewLogMapper;

    public LoggingService(
        LoginLogMapper loginLogMapper,
        OperationLogMapper operationLogMapper,
        ProductViewLogMapper productViewLogMapper
    ) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.productViewLogMapper = productViewLogMapper;
    }

    public void recordLogin(
        Long userId,
        String emailSnapshot,
        String roleSnapshot,
        String loginResult,
        String ipAddress,
        String userAgent
    ) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setUserId(userId);
        entity.setEmailSnapshot(emailSnapshot);
        entity.setRoleSnapshot(roleSnapshot);
        entity.setLoginResult(loginResult);
        entity.setIpAddress(ipAddress);
        entity.setUserAgent(userAgent);
        loginLogMapper.insert(entity);
    }

    public void recordOperation(
        Long operatorUserId,
        String operatorRole,
        String actionType,
        String targetType,
        String targetId,
        String actionDetail,
        String ipAddress
    ) {
        if (operatorUserId == null || targetId == null || targetId.isBlank()) {
            return;
        }

        OperationLogEntity entity = new OperationLogEntity();
        entity.setOperatorUserId(operatorUserId);
        entity.setOperatorRole(operatorRole);
        entity.setActionType(actionType);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setActionDetail(actionDetail == null || actionDetail.isBlank() ? actionType : actionDetail);
        entity.setIpAddress(ipAddress);
        operationLogMapper.insert(entity);
    }

    public void recordProductView(Long userId, String anonymousId, Long productId, Long categoryId) {
        if (userId == null && (anonymousId == null || anonymousId.isBlank())) {
            throw new IllegalArgumentException("anonymousId is required for anonymous view logs");
        }

        ProductViewLogEntity entity = new ProductViewLogEntity();
        entity.setUserId(userId);
        entity.setAnonymousId(anonymousId);
        entity.setProductId(productId);
        entity.setCategoryId(categoryId);
        productViewLogMapper.insert(entity);
    }
}
