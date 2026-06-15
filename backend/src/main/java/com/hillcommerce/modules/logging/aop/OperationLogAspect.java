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

/**
 * 操作日志 AOP 切面，拦截所有标注了 {@link OperationLog} 的方法。
 *
 * <h3>执行时机与顺序</h3>
 * <ol>
 *   <li>先让目标方法执行（{@code joinPoint.proceed()}）。</li>
 *   <li>目标方法成功返回后，异步记录操作日志。</li>
 * </ol>
 * 这样设计是为了保证即使日志记录失败，也不影响目标方法的执行结果。
 *
 * <h3>日志内容</h3>
 * 每条操作日志包含：
 * <ul>
 *   <li><b>操作人</b> — 从 Spring Security 上下文中提取当前登录用户的 ID 和角色</li>
 *   <li><b>操作类型</b> — {@link OperationLog#action()}，如 "CREATE"、"UPDATE"、"DELETE"</li>
 *   <li><b>目标类型</b> — {@link OperationLog#targetType()}，如 "PRODUCT"、"ORDER"</li>
 *   <li><b>目标 ID</b> — 通过 {@link OperationLog#targetIdExpr()} 的 SpEL 表达式从方法参数
 *       或返回值中提取，表达式可引用 {@code #paramName}（方法参数）和 {@code #result}（返回值）</li>
 *   <li><b>操作详情</b> — {@link OperationLog#detail()}</li>
 *   <li><b>客户端 IP</b> — 优先取 {@code X-Forwarded-For} 头</li>
 * </ul>
 *
 * <h3>容错设计</h3>
 * 日志记录过程完全被 try-catch 包裹，任何异常只打 error 日志，不向上抛。
 * 这意味着即使数据库写入失败或 SpEL 解析出错，业务请求仍然正常返回。
 *
 * <h3>匿名请求</h3>
 * 如果当前请求没有认证主体（未登录），则静默跳过，不记录日志。
 *
 * @see OperationLog
 * @see LoggingService
 */
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

    /**
     * 环绕通知，先执行业务方法，成功后再记录操作日志。
     * 日志记录失败不影响业务返回值。
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        // 先执行目标方法，确保业务不受日志记录影响
        Object result = joinPoint.proceed();

        try {
            AuthenticatedUserPrincipal principal = resolvePrincipal();
            // 匿名请求不记录日志
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
            // 日志记录失败不能影响业务
            log.error("Failed to write operation log", exception);
        }

        return result;
    }

    /**
     * 从 SpEL 表达式解析目标 ID。表达式可引用：
     * <ul>
     *   <li>{@code #paramName} — 方法参数（需要 {@code -parameters} 编译选项）</li>
     *   <li>{@code #result} — 目标方法的返回值</li>
     * </ul>
     * 表达式为空时返回空字符串。
     */
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

    /** 从当前请求上下文提取已认证用户主体，未认证时返回 null */
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

    /** 将角色列表转为逗号分隔字符串，空集合写 UNKNOWN */
    private String summarizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "UNKNOWN";
        }
        return String.join(",", roles);
    }

    /**
     * 解析客户端真实 IP。优先从 {@code X-Forwarded-For} 头取第一个 IP（最上游代理写入的
     * 原始客户端 IP），回退到 {@code request.getRemoteAddr()}。
     */
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
