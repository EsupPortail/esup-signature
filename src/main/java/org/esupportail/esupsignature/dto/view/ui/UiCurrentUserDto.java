package org.esupportail.esupsignature.dto.view.ui;

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
}

