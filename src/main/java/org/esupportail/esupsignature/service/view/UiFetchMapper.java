package org.esupportail.esupsignature.service.view;

import org.esupportail.esupsignature.dto.view.ui.AdminUiStatusDto;
import org.esupportail.esupsignature.dto.view.FrontendGlobalProperties;
import org.esupportail.esupsignature.dto.view.ui.ExternalSignatureParamsDto;
import org.esupportail.esupsignature.dto.view.ui.SuUserDto;
import org.esupportail.esupsignature.dto.view.ui.UiConfigDto;
import org.esupportail.esupsignature.dto.view.ui.UiCountersDto;
import org.esupportail.esupsignature.dto.view.ui.UiDataDto;
import org.esupportail.esupsignature.dto.view.ui.UiGlobalPropertiesDto;
import org.esupportail.esupsignature.dto.view.ui.UiHomeBootstrapDto;
import org.esupportail.esupsignature.dto.view.ui.UiMeDto;
import org.esupportail.esupsignature.dto.view.ui.UserShellDto;
import org.esupportail.esupsignature.entity.SignRequestParams;
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
public class UiFetchMapper {

    public AdminUiStatusDto toAdminUiStatusDto(Integer nbSessions, Boolean dssStatus) {
        return new AdminUiStatusDto(nbSessions, dssStatus);
    }

    public UiDataDto toUiDataDto(UiConfigDto config,
                                 UiCountersDto counters,
                                 UiMeDto currentUser,
                                 Map<String, String> preferences,
                                 AdminUiStatusDto adminStatus) {
        return new UiDataDto(config, counters, currentUser, preferences, adminStatus);
    }

    public UiHomeBootstrapDto toUiHomeBootstrapDto(Long startFormId,
                                                   Long startWorkflowId,
                                                   String warningReadUrl,
                                                   String searchUrl,
                                                   String searchTitlesUrl,
                                                   List<UiHomeBootstrapDto.SignBookItem> toSignSignBooks,
                                                   List<UiHomeBootstrapDto.SignBookItem> pendingSignBooks) {
        return new UiHomeBootstrapDto(startFormId, startWorkflowId, warningReadUrl, searchUrl, searchTitlesUrl, toSignSignBooks, pendingSignBooks);
    }

    public UiMeDto toUiMeDto(User user,
                             Set<String> userRoles,
                             User authUser,
                             Set<String> authUserRoles,
                             List<User> suUsers,
                             List<Long> userImagesIds,
                             String keystoreFileName,
                             Map<UiParams, String> uiParams,
                             Object securityServiceName) {
        return new UiMeDto(
                toUserShellDto(user, userRoles),
                toUserShellDto(authUser, authUserRoles),
                suUsers == null ? List.of() : suUsers.stream().map(this::toSuUserDto).toList(),
                userImagesIds == null ? List.of() : List.copyOf(userImagesIds),
                keystoreFileName,
                toUiParamsMap(uiParams),
                securityServiceName != null ? securityServiceName.toString() : null
        );
    }

    public UserShellDto toUserShellDto(User user, Set<String> roles) {
        if (user == null) {
            return null;
        }
        List<String> sortedRoles = roles == null
                ? List.of()
                : roles.stream().sorted().toList();
        return new UserShellDto(
                user.getId(),
                user.getEppn(),
                user.getFirstname(),
                user.getName(),
                user.getEmail(),
                user.getUserType() != null ? user.getUserType().name() : null,
                user.getDefaultSignImageNumber(),
                sortedRoles
        );
    }

    public SuUserDto toSuUserDto(User user) {
        if (user == null) {
            return null;
        }
        return new SuUserDto(
                user.getEppn(),
                user.getFirstname(),
                user.getName(),
                user.getUserShareId()
        );
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
        return new UiCountersDto(
                nbSignRequests,
                nbToSign,
                nbDeleted,
                reportNumber,
                managedWorkflowsSize,
                isRoleManager,
                isOneSignShare,
                isOneReadShare,
                certificatProblem
        );
    }

    public UiConfigDto toUiConfigDto(FrontendGlobalProperties globalProperties,
                                     Boolean enableSms,
                                     Boolean validationToolsEnabled,
                                     String applicationEmail,
                                     Integer maxInactiveInterval,
                                     Integer hoursBeforeRefreshNotif,
                                     Boolean infiniteScrolling,
                                     String versionApp,
                                     String profile) {
        return new UiConfigDto(
                toUiGlobalPropertiesDto(globalProperties),
                enableSms,
                validationToolsEnabled,
                applicationEmail,
                maxInactiveInterval,
                hoursBeforeRefreshNotif,
                infiniteScrolling,
                versionApp,
                profile
        );
    }

    public UiGlobalPropertiesDto toUiGlobalPropertiesDto(FrontendGlobalProperties props) {
        if (props == null) {
            return null;
        }
        return new UiGlobalPropertiesDto(
                props.getRootUrl(),
                props.getDomain(),
                props.getHideWizard(),
                props.getHideWizardWorkflow(),
                props.getHideAutoSign(),
                props.getHideSendSignRequest(),
                props.getApplicationEmail(),
                props.getInfiniteScrolling(),
                props.getReturnToHomeAfterSign(),
                props.getNamingTemplate(),
                props.getSignedSuffix(),
                props.getMaxUploadSize(),
                props.getPdfOnly(),
                props.getExportAttachements(),
                props.getNexuUrl(),
                props.getAuthorizedSignTypes() == null ? List.of() : List.copyOf(props.getAuthorizedSignTypes()),
                props.getSignatureImageDpi(),
                props.getFixFactor(),
                props.getExternalCanEdit(),
                props.getHideHiddenVisa(),
                props.getDisablePdfFontAlert(),
                props.getDefaultFontSize(),
                props.getEnableHelp(),
                props.getOtpValidity(),
                props.getSmsRequired(),
                props.getNbSignOtpTries(),
                props.getNbViewOtpTries(),
                props.getEnableTransfertForUsers(),
                props.getEnableCaptcha(),
                props.getEnableSu(),
                props.getShareMode(),
                toExternalSignatureParamsDto(props.getExternalSignatureParams()),
                props.getNbDaysBeforeWarning(),
                props.getNbDaysBeforeDeleting(),
                props.getNewVersion(),
                props.getDisableCertStorage(),
                props.getSealCertificatConfigured(),
                props.getSealDriverConfigured()
        );
    }

    public ExternalSignatureParamsDto toExternalSignatureParamsDto(SignRequestParams params) {
        if (params == null) {
            return null;
        }
        return new ExternalSignatureParamsDto(
                params.getAddWatermark(),
                params.getExtraDate(),
                params.getExtraType(),
                params.getExtraName(),
                params.getAddExtra(),
                params.getExtraText(),
                params.getExtraOnTop()
        );
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


