package com.hillcommerce.modules.order.web;

import static com.hillcommerce.modules.order.web.OrderCenterDtos.OrderListQuery;
import static com.hillcommerce.modules.order.web.OrderDtos.OrderDetailResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.AdminOrderListResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.AutoCompleteResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.ConfirmReceiptResponse;
import static com.hillcommerce.modules.order.web.ShipmentDtos.ShipOrderRequest;
import static com.hillcommerce.modules.order.web.ShipmentDtos.ShipOrderResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.hillcommerce.modules.order.service.ShipmentService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/api/admin/orders/{orderId}/ship")
    public OrderDetailResponse getShipOrder(@PathVariable Long orderId, Authentication authentication) {
        requireStaff(authentication);
        return shipmentService.getShipOrderDetail(orderId);
    }

    @PostMapping("/api/admin/orders/{orderId}/ship")
    public ShipOrderResponse shipOrder(
        @PathVariable Long orderId,
        @RequestBody ShipOrderRequest request,
        Authentication authentication
    ) {
        return shipmentService.shipOrder(requireStaff(authentication), orderId, request.carrierName(), request.trackingNo());
    }

    @PostMapping("/api/orders/{orderId}/receive")
    public ConfirmReceiptResponse confirmReceipt(@PathVariable Long orderId, Authentication authentication) {
        return shipmentService.confirmReceipt(requireUserId(authentication), orderId);
    }

    @PostMapping("/api/admin/orders/auto-complete")
    public AutoCompleteResponse autoComplete(Authentication authentication) {
        requireStaff(authentication);
        return shipmentService.autoComplete();
    }

    @GetMapping("/api/admin/orders")
    public AdminOrderListResponse listOrders(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String orderNo,
        Authentication authentication
    ) {
        requireStaff(authentication);
        return shipmentService.listAllOrders(new OrderListQuery(page, size, status, orderNo));
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return principal.id();
    }

    private Long requireStaff(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        boolean allowed = principal.roles().stream().anyMatch(role -> "ADMIN".equals(role) || "SALES".equals(role));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return principal.id();
    }
}
