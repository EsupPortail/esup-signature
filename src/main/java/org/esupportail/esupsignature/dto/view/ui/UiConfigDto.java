package org.esupportail.esupsignature.dto.view.ui;

public record UiConfigDto(
        UiGlobalPropertiesDto globalProperties,
        Boolean enableSms,
        Boolean validationToolsEnabled,
        String applicationEmail,
        Integer maxInactiveInterval,
        Integer hoursBeforeRefreshNotif,
        Boolean infiniteScrolling,
        String versionApp,
        String profile
) {
}

