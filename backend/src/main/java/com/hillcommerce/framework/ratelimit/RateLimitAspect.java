package com.hillcommerce.framework.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);
    private static final Pattern SPEL_TEMPLATE = Pattern.compile("#\\{([^}]*)\\}");

    private final RateLimitProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitBucketProvider bucketProvider;
    private final ExpressionParser spelParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(RateLimitProperties properties,
                           ClientIpResolver clientIpResolver,
                           RateLimitBucketProvider bucketProvider) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.bucketProvider = bucketProvider;
    }

    @Around("(@within(com.hillcommerce.framework.ratelimit.RateLimit) || "
          + "@within(com.hillcommerce.framework.ratelimit.RateLimit.RateLimits) || "
          + "@annotation(com.hillcommerce.framework.ratelimit.RateLimit) || "
          + "@annotation(com.hillcommerce.framework.ratelimit.RateLimit.RateLimits))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.enabled()) {
            return joinPoint.proceed();
        }

        RateLimit[] annotations = resolveAnnotations(joinPoint);
        if (annotations.length == 0) {
            return joinPoint.proceed();
        }

        String clientIp = resolveClientIp();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EvaluationContext spelContext = buildSpelContext(joinPoint, clientIp, authentication);

        for (RateLimit rateLimit : annotations) {
            String key = resolveKey(rateLimit.key(), spelContext);
            if (key == null) {
                log.warn("Rate limit key resolved to null for expression '{}', skipping this bucket",
                    rateLimit.key());
                continue;
            }

            ConsumptionProbe probe;
            try {
                probe = bucketProvider.tryConsumeAndReturnRemaining(key, rateLimit);
            } catch (Exception e) {
                log.warn("Bucket provider failed for key={}, failing open", key, e);
                continue;
            }

            if (!probe.isConsumed()) {
                throw new RateLimitExceededException(
                    rateLimit.message(), probe.getNanosToWaitForRefill());
            }
        }

        return joinPoint.proceed();
    }

    // ── annotation resolution ──

    private RateLimit[] resolveAnnotations(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(),
            joinPoint.getTarget().getClass());

        Set<RateLimit> methodAnnotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(
            method, RateLimit.class);
        if (!methodAnnotations.isEmpty()) {
            return methodAnnotations.toArray(RateLimit[]::new);
        }

        Set<RateLimit> classAnnotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(
            joinPoint.getTarget().getClass(), RateLimit.class);
        return classAnnotations.toArray(RateLimit[]::new);
    }

    // ── SpEL context ──

    private EvaluationContext buildSpelContext(ProceedingJoinPoint joinPoint,
                                               String clientIp, Authentication authentication) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        context.setVariable("clientIp", clientIp);
        context.setVariable("authentication", authentication);

        // principalKey: 已登录 → user:<id>, 未登录 → ip:<ip>
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            context.setVariable("userId", principal.id());
            context.setVariable("principalKey", "user:" + principal.id());
        } else {
            context.setVariable("principalKey", "ip:" + clientIp);
        }

        // 方法参数注入（需要 -parameters 编译选项）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(),
            joinPoint.getTarget().getClass());
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        return context;
    }

    // ── SpEL template resolution ──

    private String resolveKey(String keyTemplate, EvaluationContext context) {
        Matcher matcher = SPEL_TEMPLATE.matcher(keyTemplate);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String expression = matcher.group(1);
            Object value;
            try {
                Expression expr = spelParser.parseExpression(expression);
                value = expr.getValue(context);
            } catch (Exception e) {
                log.error("SpEL evaluation failed for '{}', cannot resolve key — "
                    + "this annotation will be skipped", expression, e);
                return null;
            }
            String replacement = value != null ? value.toString() : "null";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        return clientIpResolver.resolve(request);
    }
}
