package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.admin.AdminUiStatusDto;
import org.esupportail.esupsignature.dto.ui.global.UiCountersDto;
import org.esupportail.esupsignature.dto.ui.global.UiCurrentUserDto;
import org.esupportail.esupsignature.dto.ui.global.UiDataDto;
import org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto;
import org.esupportail.esupsignature.dto.ui.global.UiHomeDto;
import org.esupportail.esupsignature.dto.ui.global.UiUserLookupDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class UiGlobalMapper {

    public AdminUiStatusDto toAdminUiStatusDto(Integer nbSessions, Boolean dssStatus) {
        AdminUiStatusDto dto = new AdminUiStatusDto();
        dto.setNbSessions(nbSessions);
        dto.setDssStatus(dssStatus);
        return dto;
    }

    public UiDataDto toUiDataDto(UiDataDto.UiConfigDto config,
                                 UiCountersDto counters,
                                 UiCurrentUserDto currentUser,
                                 Map<String, String> preferences,
                                 AdminUiStatusDto adminStatus) {
        UiDataDto dto = new UiDataDto();
        dto.setConfig(config);
        dto.setCounters(counters);
        dto.setCurrentUser(currentUser);
        dto.setPreferences(preferences);
        dto.setAdminStatus(adminStatus);
        return dto;
    }

    public UiHomeDto toUiHomeBootstrapDto(Long startFormId,
                                          Long startWorkflowId,
                                          String warningReadUrl,
                                          String searchUrl,
                                          String searchTitlesUrl,
                                          List<UiHomeDto.SignBookItem> toSignSignBooks,
                                          List<UiHomeDto.SignBookItem> pendingSignBooks) {
        UiHomeDto dto = new UiHomeDto();
        dto.setStartFormId(startFormId);
        dto.setStartWorkflowId(startWorkflowId);
        dto.setWarningReadUrl(warningReadUrl);
        dto.setSearchUrl(searchUrl);
        dto.setSearchTitlesUrl(searchTitlesUrl);
        dto.setToSignSignBooks(toSignSignBooks);
        dto.setPendingSignBooks(pendingSignBooks);
        return dto;
    }

    public UiCurrentUserDto toUiMeDto(User user,
                                      Set<String> userRoles,
                                      User authUser,
                                      Set<String> authUserRoles,
                                      List<User> suUsers,
                                      List<Long> userImagesIds,
                                      String keystoreFileName,
                                      Map<UiParams, String> uiParams,
                                      Object securityServiceName) {
        UiCurrentUserDto dto = new UiCurrentUserDto();
        dto.setUser(toUiUserDto(user, userRoles));
        dto.setAuthUser(toUiUserDto(authUser, authUserRoles));
        dto.setSuUsers(suUsers == null ? List.of() : suUsers.stream().map(this::toSuUserDto).toList());
        dto.setUserImagesIds(userImagesIds == null ? List.of() : List.copyOf(userImagesIds));
        dto.setKeystoreFileName(keystoreFileName);
        dto.setUiParams(toUiParamsMap(uiParams));
        dto.setSecurityServiceName(securityServiceName != null ? securityServiceName.toString() : null);
        return dto;
    }

    public UiCurrentUserDto.UiUserDto toUiUserDto(User user, Set<String> roles) {
        if (user == null) {
            return null;
        }
        List<String> sortedRoles = roles == null
                ? List.of()
                : roles.stream().sorted().toList();
        UiCurrentUserDto.UiUserDto dto = new UiCurrentUserDto.UiUserDto();
        dto.setId(user.getId());
        dto.setEppn(user.getEppn());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        dto.setDefaultSignImageNumber(user.getDefaultSignImageNumber());
        dto.setRoles(sortedRoles);
        return dto;
    }

    public UiCurrentUserDto.SuUserDto toSuUserDto(User user) {
        if (user == null) {
            return null;
        }
        UiCurrentUserDto.SuUserDto dto = new UiCurrentUserDto.SuUserDto();
        dto.setEppn(user.getEppn());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        dto.setUserShareId(user.getUserShareId());
        return dto;
    }

    public UiUserLookupDto toUiUserLookupDto(User user) {
        if (user == null) {
            return null;
        }
        UiUserLookupDto dto = new UiUserLookupDto();
        dto.setEmail(user.getEmail());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        dto.setHidedPhone(user.getHidedPhone());
        return dto;
    }

    public List<UiUserLookupDto> toUiUserLookupDtos(List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream()
                .map(this::toUiUserLookupDto)
                .toList();
    }

    public List<UiUserLookupDto> toUiUserLookupDtos(Set<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream()
                .map(this::toUiUserLookupDto)
                .toList();
    }

    public UiCountersDto toUiCountersDto(Long nbSignRequests,
                                         Long nbToSign,
                                         Long nbDeleted,
                                         Integer reportNumber,
                                         Integer managedWorkflowsSize,
                                         Boolean isRoleManager,
                                         Boolean isOneSignShare,
                                         Boolean isOneReadShare,
                                         Boolean certificatProblem) {
        UiCountersDto dto = new UiCountersDto();
        dto.setNbSignRequests(nbSignRequests);
        dto.setNbToSign(nbToSign);
        dto.setNbDeleted(nbDeleted);
        dto.setReportNumber(reportNumber);
        dto.setManagedWorkflowsSize(managedWorkflowsSize);
        dto.setIsRoleManager(isRoleManager);
        dto.setIsOneSignShare(isOneSignShare);
        dto.setIsOneReadShare(isOneReadShare);
        dto.setCertificatProblem(certificatProblem);
        return dto;
    }

    public UiDataDto.UiConfigDto toUiConfigDto(UiGlobalPropertiesDto globalProperties,
                                               String enableSms,
                                               Boolean smsRequired,
                                               Boolean validationToolsEnabled,
                                               String applicationEmail,
                                               Integer maxInactiveInterval,
                                               Integer hoursBeforeRefreshNotif,
                                               Boolean infiniteScrolling,
                                               String versionApp,
                                               String profile) {
        UiDataDto.UiConfigDto dto = new UiDataDto.UiConfigDto();
        dto.setGlobalProperties(globalProperties);
        dto.setEnableSms(enableSms);
        dto.setSmsRequired(smsRequired);
        dto.setValidationToolsEnabled(validationToolsEnabled);
        dto.setApplicationEmail(applicationEmail);
        dto.setMaxInactiveInterval(maxInactiveInterval);
        dto.setHoursBeforeRefreshNotif(hoursBeforeRefreshNotif);
        dto.setInfiniteScrolling(infiniteScrolling);
        dto.setVersionApp(versionApp);
        dto.setProfile(profile);
        return dto;
    }

    public Map<String, String> toUiParamsMap(Map<UiParams, String> uiParams) {
        if (uiParams == null || uiParams.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> mappedUiParams = new LinkedHashMap<>();
        uiParams.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .forEach(entry -> mappedUiParams.put(entry.getKey().name(), entry.getValue()));
        return mappedUiParams;
    }
}
