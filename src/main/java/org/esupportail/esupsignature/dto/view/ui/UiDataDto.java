package org.esupportail.esupsignature.dto.view.ui;

import java.util.Map;

public record UiDataDto(
        UiConfigDto config,
        UiCountersDto counters,
        UiMeDto currentUser,
        Map<String, String> preferences,
        AdminUiStatusDto adminStatus
) {
}


