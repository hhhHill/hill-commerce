package com.hillcommerce.modules.logging.aop;

import java.lang.reflect.Method;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.hillcommerce.modules.logging.service.LoggingService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final LoggingService loggingService;
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public OperationLogAspect(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            AuthenticatedUserPrincipal principal = resolvePrincipal();
            if (principal == null) {
                return result;
            }

            String targetId = resolveTargetId(joinPoint, result, operationLog.targetIdExpr());
            loggingService.recordOperation(
                principal.id(),
                summarizeRoles(principal.roles()),
                operationLog.action(),
                operationLog.targetType(),
                targetId,
                operationLog.detail(),
                resolveRemoteAddr());
        } catch (Exception exception) {
            log.error("Failed to write operation log", exception);
        }

        return result;
    }

    private String resolveTargetId(ProceedingJoinPoint joinPoint, Object result, String targetIdExpr) {
        if (targetIdExpr == null || targetIdExpr.isBlank()) {
            return "";
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int index = 0; index < parameterNames.length; index++) {
                context.setVariable(parameterNames[index], args[index]);
            }
        }
        context.setVariable("result", result);

        Object evaluated = expressionParser.parseExpression(targetIdExpr).getValue(context);
        return evaluated == null ? "" : String.valueOf(evaluated);
    }

    private AuthenticatedUserPrincipal resolvePrincipal() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        Object principal = attributes.getRequest().getUserPrincipal();
        if (principal instanceof org.springframework.security.core.Authentication authentication
            && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
            return authenticatedUserPrincipal;
        }
        return null;
    }

    private String summarizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "UNKNOWN";
        }
        return String.join(",", roles);
    }

    private String resolveRemoteAddr() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
