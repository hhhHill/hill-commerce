package com.hillcommerce.modules.user.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserAddressDtos {

    private UserAddressDtos() {
    }

    public record UserAddressRequest(
        @NotBlank @Size(max = 64) String receiverName,
        @NotBlank @Size(max = 32) String receiverPhone,
        @NotBlank @Size(max = 64) String province,
        @NotBlank @Size(max = 64) String city,
        @NotBlank @Size(max = 64) String district,
        @NotBlank @Size(max = 255) String detailAddress,
        @Size(max = 16) String postalCode
    ) {
    }

    public record UserAddressResponse(
        Long id,
        String receiverName,
        String receiverPhone,
        String province,
        String city,
        String district,
        String detailAddress,
        String postalCode,
        Boolean isDefault
    ) {
    }
}
