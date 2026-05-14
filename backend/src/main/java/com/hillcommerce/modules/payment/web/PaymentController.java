package com.hillcommerce.modules.payment.web;

import static com.hillcommerce.modules.payment.web.PaymentDtos.PaymentAttemptResponse;
import static com.hillcommerce.modules.payment.web.PaymentDtos.PaymentActionResponse;
import static com.hillcommerce.modules.payment.web.PaymentDtos.CloseExpiredPaymentsResponse;
import static com.hillcommerce.modules.payment.web.PaymentDtos.PaymentOrderResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.payment.service.PaymentService;
import com.hillcommerce.modules.payment.service.PaymentCloseService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentCloseService paymentCloseService;

    public PaymentController(PaymentService paymentService, PaymentCloseService paymentCloseService) {
        this.paymentService = paymentService;
        this.paymentCloseService = paymentCloseService;
    }

    @GetMapping("/orders/{orderId}")
    public PaymentOrderResponse getPaymentOrder(@PathVariable Long orderId, Authentication authentication) {
        return paymentService.getPaymentOrder(requireUserId(authentication), orderId);
    }

    @PostMapping("/orders/{orderId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentAttemptResponse createAttempt(@PathVariable Long orderId, Authentication authentication) {
        return paymentService.createOrReuseAttempt(requireUserId(authentication), orderId);
    }

    @PostMapping("/{paymentId}/succeed")
    public PaymentActionResponse succeed(@PathVariable Long paymentId, Authentication authentication) {
        return paymentService.succeed(requireUserId(authentication), paymentId);
    }

    @PostMapping("/{paymentId}/fail")
    public PaymentActionResponse fail(@PathVariable Long paymentId, Authentication authentication) {
        return paymentService.fail(requireUserId(authentication), paymentId);
    }

    @PostMapping("/close-expired")
    public CloseExpiredPaymentsResponse closeExpired(Authentication authentication) {
        requireStaff(authentication);
        return paymentCloseService.closeExpiredPayments();
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return principal.id();
    }

    private void requireStaff(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        boolean allowed = principal.roles().stream().anyMatch(role -> "ADMIN".equals(role) || "SALES".equals(role));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }
}
