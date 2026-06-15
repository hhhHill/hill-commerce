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

/**
 * 限流 AOP 切面，拦截所有标注了 {@link RateLimit} 的方法。
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li>检查全局开关 {@code hill.rate-limit.enabled}，关闭则直接放行。</li>
 *   <li>从方法或类上收集所有 {@link RateLimit} 注解（支持可重复注解，一个方法可配置多个限流维度）。</li>
 *   <li>解析客户端 IP（优先从 {@code X-Forwarded-For} / {@code X-Real-IP} 取，适配反向代理）。</li>
 *   <li>构建 SpEL 上下文，注入 {@code clientIp}、{@code authentication}、{@code userId}、
 *       {@code principalKey} 以及所有方法参数。</li>
 *   <li>对每个 {@link RateLimit}，解析其 SpEL key 模板，从 {@link RateLimitBucketProvider}
 *       尝试消费 1 个令牌。</li>
 *   <li>任一桶消费失败则抛出 {@link RateLimitExceededException}，由全局异常处理器转为 HTTP 429。</li>
 * </ol>
 *
 * <h3>失效开放</h3>
 * 如果桶提供程序抛异常（如 Redis 不可用），记录警告日志后跳过该桶继续处理后续注解，
 * 不会阻断正常请求。这保证了限流组件故障时系统仍可用。
 *
 * <h3>SpEL 模板</h3>
 * key 使用 {@code #{...}} 包裹 SpEL 表达式，而非 Spring Security 标准的花括号语法，
 * 避免与 YAML 占位符冲突。表达式通过正则提取后交给 SpEL 引擎求值。
 *
 * @see RateLimit
 * @see RateLimitBucketProvider
 * @see RateLimitProperties
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /** 匹配 {@code #{expression}} 模板，提取花括号内的 SpEL 表达式 */
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

    /**
     * 核心环绕通知。切入所有类级或方法级标注了 {@link RateLimit} / {@link RateLimit.RateLimits} 的方法。
     */
    @Around("(@within(com.hillcommerce.framework.ratelimit.RateLimit) || "
          + "@within(com.hillcommerce.framework.ratelimit.RateLimit.RateLimits) || "
          + "@annotation(com.hillcommerce.framework.ratelimit.RateLimit) || "
          + "@annotation(com.hillcommerce.framework.ratelimit.RateLimit.RateLimits))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 全局开关：关闭时限流完全旁路
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

        // 逐个检查每个限流桶，任一桶耗尽即拒绝请求
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
                // 失效开放：桶提供程序故障时放行，避免限流组件拖垮业务
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

    /**
     * 解析要生效的限流注解。方法级注解优先于类级注解：如果方法上找到了至少一个
     * {@link RateLimit}，则忽略类级注解；否则使用类级注解。这与 Spring 的可重复注解
     * 合并语义一致。
     */
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

    /**
     * 构建 SpEL 求值上下文，注入以下变量供 key 表达式使用：
     * <ul>
     *   <li>{@code #clientIp} — 解析后的客户端 IP</li>
     *   <li>{@code #authentication} — Spring Security 认证对象（可能为 null）</li>
     *   <li>{@code #userId} — 已登录用户的 ID，匿名时为 null</li>
     *   <li>{@code #principalKey} — 已登录时返回 {@code user:<id>}，否则返回 {@code ip:<ip>}</li>
     *   <li>所有方法参数名 — 需要编译时开启 {@code -parameters}（Spring Boot 默认开启）</li>
     * </ul>
     */
    private EvaluationContext buildSpelContext(ProceedingJoinPoint joinPoint,
                                               String clientIp, Authentication authentication) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        context.setVariable("clientIp", clientIp);
        context.setVariable("authentication", authentication);

        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            context.setVariable("userId", principal.id());
            context.setVariable("principalKey", "user:" + principal.id());
        } else {
            context.setVariable("principalKey", "ip:" + clientIp);
        }

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

    /**
     * 解析 key 模板。模板格式为 {@code login:ip:#{#clientIp}}，其中
     * {@code #{...}} 内为 SpEL 表达式。解析失败时返回 null，
     * 调用方跳过该桶并记录警告。
     */
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

    /** 从当前请求上下文解析客户端真实 IP（处理反向代理场景） */
    private String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        return clientIpResolver.resolve(request);
    }
}
