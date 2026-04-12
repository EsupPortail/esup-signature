package org.esupportail.esupsignature.dto.view.ui;

import java.util.List;

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


