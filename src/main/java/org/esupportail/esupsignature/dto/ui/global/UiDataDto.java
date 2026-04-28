package org.esupportail.esupsignature.dto.ui.global;

import org.esupportail.esupsignature.dto.page.admin.AdminUiStatusDto;

import java.util.Map;

public record UiDataDto(
        UiConfigDto config,
        UiCountersDto counters,
        UiCurrentUserDto currentUser,
        Map<String, String> preferences,
        AdminUiStatusDto adminStatus
) {

    public record UiConfigDto(
            UiGlobalPropertiesDto globalProperties,
            String enableSms,
            Boolean smsRequired,
            Boolean validationToolsEnabled,
            String applicationEmail,
            Integer maxInactiveInterval,
            Integer hoursBeforeRefreshNotif,
            Boolean infiniteScrolling,
            String versionApp,
            String profile
    ) {
    }

}


