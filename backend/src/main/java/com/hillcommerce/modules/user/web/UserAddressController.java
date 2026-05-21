package com.hillcommerce.modules.user.web;

import static com.hillcommerce.modules.user.dto.UserAddressDtos.UserAddressRequest;
import static com.hillcommerce.modules.user.dto.UserAddressDtos.UserAddressResponse;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;
import com.hillcommerce.modules.user.service.UserAddressService;

@RestController
@RequestMapping("/api/user/addresses")
public class UserAddressController {

    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @GetMapping
    @RequireRole("CUSTOMER")
    public List<UserAddressResponse> listAddresses(Authentication authentication) {
        return userAddressService.listAddresses(requireUserId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole("CUSTOMER")
    public UserAddressResponse createAddress(
        Authentication authentication,
        @Valid @RequestBody UserAddressRequest request
    ) {
        return userAddressService.createAddress(requireUserId(authentication), request);
    }

    @PutMapping("/{addressId}")
    @RequireRole("CUSTOMER")
    public UserAddressResponse updateAddress(
        Authentication authentication,
        @PathVariable Long addressId,
        @Valid @RequestBody UserAddressRequest request
    ) {
        return userAddressService.updateAddress(requireUserId(authentication), addressId, request);
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireRole("CUSTOMER")
    public void deleteAddress(Authentication authentication, @PathVariable Long addressId) {
        userAddressService.deleteAddress(requireUserId(authentication), addressId);
    }

    @PutMapping("/{addressId}/default")
    @RequireRole("CUSTOMER")
    public UserAddressResponse setDefaultAddress(Authentication authentication, @PathVariable Long addressId) {
        return userAddressService.setDefaultAddress(requireUserId(authentication), addressId);
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED, "Authenticated user is required");
        }
        return principal.id();
    }
}
