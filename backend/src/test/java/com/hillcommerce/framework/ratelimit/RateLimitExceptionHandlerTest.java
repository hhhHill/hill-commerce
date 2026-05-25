package com.hillcommerce.framework.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.framework.web.RateLimitExceptionHandler;

class RateLimitExceptionHandlerTest {

    @RestController
    static class Test12sController {
        @GetMapping("/test-429-12s")
        String trigger12s() {
            throw new RateLimitExceededException("请稍后再试", 12_000_000_000L);
        }
    }

    @RestController
    static class TestSubSecondController {
        @GetMapping("/test-429-subsecond")
        String triggerSubsecond() {
            throw new RateLimitExceededException("稍后", 500_000L);
        }
    }

    @RestController
    static class Test12_1sController {
        @GetMapping("/test-429-12_1s")
        String trigger12_1s() {
            throw new RateLimitExceededException("稍后", 12_100_000_000L);
        }
    }

    private final MockMvc mockMvc12s = MockMvcBuilders
        .standaloneSetup(new Test12sController())
        .setControllerAdvice(new RateLimitExceptionHandler())
        .build();

    private final MockMvc mockMvcSubSecond = MockMvcBuilders
        .standaloneSetup(new TestSubSecondController())
        .setControllerAdvice(new RateLimitExceptionHandler())
        .build();

    private final MockMvc mockMvc12_1s = MockMvcBuilders
        .standaloneSetup(new Test12_1sController())
        .setControllerAdvice(new RateLimitExceptionHandler())
        .build();

    @Test
    void shouldReturn429With12sRetryAfter() throws Exception {
        mockMvc12s.perform(get("/test-429-12s"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value(ErrorCode.RATE_LIMIT_EXCEEDED.code()))
            .andExpect(jsonPath("$.message").value("请稍后再试"))
            .andExpect(jsonPath("$.retryAfter").value(12))
            .andExpect(header().string("Retry-After", "12"));
    }

    @Test
    void shouldCeilToAtLeast1Second() throws Exception {
        mockMvcSubSecond.perform(get("/test-429-subsecond"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.retryAfter").value(1))
            .andExpect(header().string("Retry-After", "1"));
    }

    @Test
    void shouldCeilTo13For12_1Seconds() throws Exception {
        mockMvc12_1s.perform(get("/test-429-12_1s"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.retryAfter").value(13))
            .andExpect(header().string("Retry-After", "13"));
    }
}
