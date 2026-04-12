package org.esupportail.esupsignature.dto.view.ui;

public record UiBootstrapDto(
        UiConfigDto config,
        UiCountersDto counters,
        UiMeDto currentUser,
        AdminUiStatusDto adminStatus
) {
}

