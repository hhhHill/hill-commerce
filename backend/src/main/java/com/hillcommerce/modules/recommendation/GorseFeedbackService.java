package com.hillcommerce.modules.recommendation;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GorseFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(GorseFeedbackService.class);

    private final GorseClient gorseClient;

    public GorseFeedbackService(GorseClient gorseClient) {
        this.gorseClient = gorseClient;
    }

    public void fireAndForgetView(Long userId, String anonymousId, Long productId) {
        fireAndForget(userKey(userId, anonymousId), productId, "view");
    }

    public void fireAndForgetPurchase(Long userId, Long productId) {
        fireAndForget(userKey(userId, null), productId, "purchase");
    }

    private void fireAndForget(String userKey, Long productId, String feedbackType) {
        if (userKey == null || productId == null) {
            return;
        }
        try {
            gorseClient.insertFeedback(new GorseClient.FeedbackPayload(
                userKey,
                itemKey(productId),
                feedbackType,
                Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("Failed to send Gorse {} feedback for product {}", feedbackType, productId, exception);
        }
    }

    static String userKey(Long userId, String anonymousId) {
        if (userId != null) {
            return "user:" + userId;
        }
        if (anonymousId == null || anonymousId.isBlank()) {
            return null;
        }
        return "anon:" + anonymousId.trim();
    }

    static String itemKey(Long productId) {
        return "product:" + productId;
    }
}
