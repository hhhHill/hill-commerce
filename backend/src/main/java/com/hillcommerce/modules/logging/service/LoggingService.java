package com.hillcommerce.modules.logging.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.logging.entity.LoginLogEntity;
import com.hillcommerce.modules.logging.entity.OperationLogEntity;
import com.hillcommerce.modules.logging.entity.ProductViewLogEntity;
import com.hillcommerce.modules.logging.mapper.LoginLogMapper;
import com.hillcommerce.modules.logging.mapper.OperationLogMapper;
import com.hillcommerce.modules.logging.mapper.ProductViewLogMapper;
import com.hillcommerce.modules.recommendation.GorseFeedbackService;

@Service
public class LoggingService {

    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final ProductViewLogMapper productViewLogMapper;
    private final GorseFeedbackService gorseFeedbackService;

    public LoggingService(
        LoginLogMapper loginLogMapper,
        OperationLogMapper operationLogMapper,
        ProductViewLogMapper productViewLogMapper,
        GorseFeedbackService gorseFeedbackService
    ) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.productViewLogMapper = productViewLogMapper;
        this.gorseFeedbackService = gorseFeedbackService;
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
            throw new BusinessException(ErrorCode.ANONYMOUS_ID_REQUIRED, "anonymousId is required for anonymous view logs");
        }

        ProductViewLogEntity entity = new ProductViewLogEntity();
        entity.setUserId(userId);
        entity.setAnonymousId(anonymousId);
        entity.setProductId(productId);
        entity.setCategoryId(categoryId);
        productViewLogMapper.insert(entity);
        gorseFeedbackService.fireAndForgetView(userId, anonymousId, productId);

        if (categoryId != null) {
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                jakarta.servlet.http.HttpSession session = attrs.getRequest().getSession(false);
                if (session != null) {
                    @SuppressWarnings("unchecked")
                    LinkedHashSet<Long> recentCategories = (LinkedHashSet<Long>) session.getAttribute("RECENT_CATEGORIES");
                    if (recentCategories == null) {
                        recentCategories = new LinkedHashSet<>();
                    }
                    if (recentCategories.size() >= 10) {
                        Long oldest = recentCategories.iterator().next();
                        recentCategories.remove(oldest);
                    }
                    recentCategories.add(categoryId);
                    session.setAttribute("RECENT_CATEGORIES", recentCategories);
                }
            } catch (RuntimeException ignored) {
                // session not available (e.g., non-web context) — silently skip
            }
        }
    }
}
