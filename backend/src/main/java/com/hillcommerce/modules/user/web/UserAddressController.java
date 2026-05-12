package com.hillcommerce.modules.user.web;

import static com.hillcommerce.modules.user.web.UserAddressDtos.UserAddressRequest;
import static com.hillcommerce.modules.user.web.UserAddressDtos.UserAddressResponse;

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
    public List<UserAddressResponse> listAddresses(Authentication authentication) {
        return userAddressService.listAddresses(requireUserId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAddressResponse createAddress(
        Authentication authentication,
        @Valid @RequestBody UserAddressRequest request
    ) {
        return userAddressService.createAddress(requireUserId(authentication), request);
    }

    @PutMapping("/{addressId}")
    public UserAddressResponse updateAddress(
        Authentication authentication,
        @PathVariable Long addressId,
        @Valid @RequestBody UserAddressRequest request
    ) {
        return userAddressService.updateAddress(requireUserId(authentication), addressId, request);
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(Authentication authentication, @PathVariable Long addressId) {
        userAddressService.deleteAddress(requireUserId(authentication), addressId);
    }

    @PutMapping("/{addressId}/default")
    public UserAddressResponse setDefaultAddress(Authentication authentication, @PathVariable Long addressId) {
        return userAddressService.setDefaultAddress(requireUserId(authentication), addressId);
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return principal.id();
    }
}
