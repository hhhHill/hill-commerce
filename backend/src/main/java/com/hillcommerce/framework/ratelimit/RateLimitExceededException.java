package com.hillcommerce.framework.ratelimit;

public class RateLimitExceededException extends RuntimeException {

    private final long nanosToWait;

    public RateLimitExceededException(String message, long nanosToWait) {
        super(message);
        this.nanosToWait = nanosToWait;
    }

    /** 等待 refill 的纳秒数 */
    public long getNanosToWait() {
        return nanosToWait;
    }
}
