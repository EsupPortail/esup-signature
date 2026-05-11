package org.esupportail.esupsignature.dto.ui.global;

import java.util.List;
import java.util.Map;

public record UiCurrentUserDto(
        UiUserDto user,
        UiUserDto authUser,
        List<SuUserDto> suUsers,
        List<Long> userImagesIds,
        String keystoreFileName,
        Map<String, String> uiParams,
        String securityServiceName
) {

    public record UiUserDto(
            Long id,
            String eppn,
            String firstname,
            String name,
            String email,
            String userType,
            Integer defaultSignImageNumber,
            List<String> roles
    ) {
    }

    public record SuUserDto(
            String eppn,
            String firstname,
            String name,
            Long userShareId
    ) {
    }

}

