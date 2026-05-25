package com.hillcommerce.framework.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 令牌桶限流注解。支持 TYPE（类级默认）和 METHOD（方法级覆盖）。
 * 当方法级存在时完全覆盖类级注解（不合并）。
 * 支持 @Repeatable，同一方法可标注多个实现多维度限流。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RateLimit.RateLimits.class)
public @interface RateLimit {

    /** 令牌桶容量（允许的最大突发） */
    long capacity() default 30;

    /** 每次补充的令牌数 */
    long refillTokens() default 10;

    /** 补充间隔 */
    long refillPeriod() default 1;

    /** 补充间隔时间单位，默认秒 */
    TimeUnit refillUnit() default TimeUnit.SECONDS;

    /**
     * 限流 bucket key 的 SpEL 模板表达式。
     * 可用变量：#clientIp, #authentication, #userId, #principalKey, 以及方法参数名
     */
    String key();

    /** 超限时的提示信息 */
    String message() default "请求过于频繁，请稍后再试";

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface RateLimits {
        RateLimit[] value();
    }
}
