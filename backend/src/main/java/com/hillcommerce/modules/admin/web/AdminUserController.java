package com.hillcommerce.modules.admin.web;

import static com.hillcommerce.modules.admin.web.AdminUserDtos.CreateSalesRequest;
import static com.hillcommerce.modules.admin.web.AdminUserDtos.ResetPasswordRequest;
import static com.hillcommerce.modules.admin.web.AdminUserDtos.SalesUserListResponse;
import static com.hillcommerce.modules.admin.web.AdminUserDtos.SalesUserResponse;
import static com.hillcommerce.modules.admin.web.AdminUserDtos.UserActionResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.admin.service.AdminUserService;
import com.hillcommerce.modules.logging.aop.OperationLog;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public SalesUserListResponse listUsers(Authentication authentication) {
        requireAdmin(authentication);
        return new SalesUserListResponse(adminUserService.listSalesUsers());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @OperationLog(action = "CREATE_USER", targetType = "USER", targetIdExpr = "#result.id")
    public SalesUserResponse createUser(
        Authentication authentication,
        @Valid @RequestBody CreateSalesRequest request
    ) {
        requireAdmin(authentication);
        return adminUserService.createSalesUser(request);
    }

    @PostMapping("/{id}/disable")
    @OperationLog(action = "DISABLE_USER", targetType = "USER", targetIdExpr = "#id")
    public UserActionResponse disableUser(@PathVariable Long id, Authentication authentication) {
        AuthenticatedUserPrincipal principal = requireAdmin(authentication);
        return adminUserService.disableSalesUser(id, principal.id());
    }

    @PostMapping("/{id}/enable")
    @OperationLog(action = "ENABLE_USER", targetType = "USER", targetIdExpr = "#id")
    public UserActionResponse enableUser(@PathVariable Long id, Authentication authentication) {
        requireAdmin(authentication);
        return adminUserService.enableSalesUser(id);
    }

    @PostMapping("/{id}/reset-password")
    @OperationLog(action = "RESET_PASSWORD", targetType = "USER", targetIdExpr = "#id")
    public UserActionResponse resetPassword(
        @PathVariable Long id,
        Authentication authentication,
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        // Password reset is a write operation that must be audit logged.
        requireAdmin(authentication);
        return adminUserService.resetPassword(id, request.password());
    }

    private AuthenticatedUserPrincipal requireAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException("forbidden");
        }
        if (!principal.roles().contains("ADMIN")) {
            throw new AccessDeniedException("forbidden");
        }
        return principal;
    }
}
