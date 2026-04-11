package org.esupportail.esupsignature.dto.view.ui;

public record SuUserDto(
        String eppn,
        String firstname,
        String name,
        Long userShareId
) {
}

