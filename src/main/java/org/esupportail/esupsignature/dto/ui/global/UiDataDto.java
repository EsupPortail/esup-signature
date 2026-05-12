package org.esupportail.esupsignature.dto.ui.global;

import org.esupportail.esupsignature.dto.page.admin.AdminUiStatusDto;

import java.util.Map;

public class UiDataDto {

    private UiConfigDto config;
    private UiCountersDto counters;
    private UiCurrentUserDto currentUser;
    private Map<String, String> preferences;
    private AdminUiStatusDto adminStatus;

    public UiDataDto() {
    }

    public UiDataDto(UiConfigDto config,
                     UiCountersDto counters,
                     UiCurrentUserDto currentUser,
                     Map<String, String> preferences,
                     AdminUiStatusDto adminStatus) {
        this.config = config;
        this.counters = counters;
        this.currentUser = currentUser;
        this.preferences = preferences;
        this.adminStatus = adminStatus;
    }

    public UiConfigDto getConfig() { return config; }
    public void setConfig(UiConfigDto config) { this.config = config; }
    public UiCountersDto getCounters() { return counters; }
    public void setCounters(UiCountersDto counters) { this.counters = counters; }
    public UiCurrentUserDto getCurrentUser() { return currentUser; }
    public void setCurrentUser(UiCurrentUserDto currentUser) { this.currentUser = currentUser; }
    public Map<String, String> getPreferences() { return preferences; }
    public void setPreferences(Map<String, String> preferences) { this.preferences = preferences; }
    public AdminUiStatusDto getAdminStatus() { return adminStatus; }
    public void setAdminStatus(AdminUiStatusDto adminStatus) { this.adminStatus = adminStatus; }

    public UiConfigDto config() { return config; }
    public UiCountersDto counters() { return counters; }
    public UiCurrentUserDto currentUser() { return currentUser; }
    public Map<String, String> preferences() { return preferences; }
    public AdminUiStatusDto adminStatus() { return adminStatus; }

    public static class UiConfigDto {

        private UiGlobalPropertiesDto globalProperties;
        private String enableSms;
        private Boolean smsRequired;
        private Boolean validationToolsEnabled;
        private String applicationEmail;
        private Integer maxInactiveInterval;
        private Integer hoursBeforeRefreshNotif;
        private Boolean infiniteScrolling;
        private String versionApp;
        private String profile;

        public UiConfigDto() {
        }

        public UiConfigDto(UiGlobalPropertiesDto globalProperties,
                           String enableSms,
                           Boolean smsRequired,
                           Boolean validationToolsEnabled,
                           String applicationEmail,
                           Integer maxInactiveInterval,
                           Integer hoursBeforeRefreshNotif,
                           Boolean infiniteScrolling,
                           String versionApp,
                           String profile) {
            this.globalProperties = globalProperties;
            this.enableSms = enableSms;
            this.smsRequired = smsRequired;
            this.validationToolsEnabled = validationToolsEnabled;
            this.applicationEmail = applicationEmail;
            this.maxInactiveInterval = maxInactiveInterval;
            this.hoursBeforeRefreshNotif = hoursBeforeRefreshNotif;
            this.infiniteScrolling = infiniteScrolling;
            this.versionApp = versionApp;
            this.profile = profile;
        }

        public UiGlobalPropertiesDto getGlobalProperties() { return globalProperties; }
        public void setGlobalProperties(UiGlobalPropertiesDto globalProperties) { this.globalProperties = globalProperties; }
        public String getEnableSms() { return enableSms; }
        public void setEnableSms(String enableSms) { this.enableSms = enableSms; }
        public Boolean getSmsRequired() { return smsRequired; }
        public void setSmsRequired(Boolean smsRequired) { this.smsRequired = smsRequired; }
        public Boolean getValidationToolsEnabled() { return validationToolsEnabled; }
        public void setValidationToolsEnabled(Boolean validationToolsEnabled) { this.validationToolsEnabled = validationToolsEnabled; }
        public String getApplicationEmail() { return applicationEmail; }
        public void setApplicationEmail(String applicationEmail) { this.applicationEmail = applicationEmail; }
        public Integer getMaxInactiveInterval() { return maxInactiveInterval; }
        public void setMaxInactiveInterval(Integer maxInactiveInterval) { this.maxInactiveInterval = maxInactiveInterval; }
        public Integer getHoursBeforeRefreshNotif() { return hoursBeforeRefreshNotif; }
        public void setHoursBeforeRefreshNotif(Integer hoursBeforeRefreshNotif) { this.hoursBeforeRefreshNotif = hoursBeforeRefreshNotif; }
        public Boolean getInfiniteScrolling() { return infiniteScrolling; }
        public void setInfiniteScrolling(Boolean infiniteScrolling) { this.infiniteScrolling = infiniteScrolling; }
        public String getVersionApp() { return versionApp; }
        public void setVersionApp(String versionApp) { this.versionApp = versionApp; }
        public String getProfile() { return profile; }
        public void setProfile(String profile) { this.profile = profile; }

        public UiGlobalPropertiesDto globalProperties() { return globalProperties; }
        public String enableSms() { return enableSms; }
        public Boolean smsRequired() { return smsRequired; }
        public Boolean validationToolsEnabled() { return validationToolsEnabled; }
        public String applicationEmail() { return applicationEmail; }
        public Integer maxInactiveInterval() { return maxInactiveInterval; }
        public Integer hoursBeforeRefreshNotif() { return hoursBeforeRefreshNotif; }
        public Boolean infiniteScrolling() { return infiniteScrolling; }
        public String versionApp() { return versionApp; }
        public String profile() { return profile; }
    }
}
