package org.esupportail.esupsignature.service.view.ui;

import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.dto.view.FrontendGlobalProperties;
import org.esupportail.esupsignature.dto.view.ui.UiConfigDto;
import org.esupportail.esupsignature.dto.view.ui.UiCountersDto;
import org.esupportail.esupsignature.dto.view.ui.UiMeDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.service.CertificatService;
import org.esupportail.esupsignature.service.ReportService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserShareService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service
public class UiFetchService {

    private final GlobalProperties globalProperties;
    private final SmsProperties smsProperties;
    private final SignRequestService signRequestService;
    private final SignBookService signBookService;
    private final WorkflowService workflowService;
    private final UserShareService userShareService;
    private final UserService userService;
    private final ReportService reportService;
    private final PreAuthorizeService preAuthorizeService;
    private final Environment environment;
    private final BuildProperties buildProperties;
    private final ValidationService validationService;
    private final CertificatService certificatService;
    private final UiFetchMapper uiFetchMapper;

    public UiFetchService(GlobalProperties globalProperties,
                          SmsProperties smsProperties,
                          SignRequestService signRequestService,
                          SignBookService signBookService,
                          WorkflowService workflowService,
                          UserShareService userShareService,
                          UserService userService,
                          ReportService reportService,
                          PreAuthorizeService preAuthorizeService,
                          Environment environment,
                          @Autowired(required = false) BuildProperties buildProperties,
                          ValidationService validationService,
                          CertificatService certificatService,
                          UiFetchMapper uiFetchMapper) {
        this.globalProperties = globalProperties;
        this.smsProperties = smsProperties;
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.workflowService = workflowService;
        this.userShareService = userShareService;
        this.userService = userService;
        this.reportService = reportService;
        this.preAuthorizeService = preAuthorizeService;
        this.environment = environment;
        this.buildProperties = buildProperties;
        this.validationService = validationService;
        this.certificatService = certificatService;
        this.uiFetchMapper = uiFetchMapper;
    }

    public UiMeDto buildUiMe(String userEppn, String authUserEppn, HttpSession httpSession) {
        if (userEppn == null) {
            return uiFetchMapper.toUiMeDto(null, Collections.emptySet(), null, Collections.emptySet(), Collections.emptyList(), Collections.emptyList(), null, Collections.emptyMap(), null);
        }
        User user = userService.getFullUserByEppn(userEppn);
        User authUser = authUserEppn != null ? userService.getByEppn(authUserEppn) : null;
        Set<String> userRoles = user != null ? userService.getRoles(userEppn) : Collections.emptySet();
        Set<String> authUserRoles = authUser != null ? userService.getRoles(authUserEppn) : Collections.emptySet();
        Map<org.esupportail.esupsignature.entity.enums.UiParams, String> uiParams = authUserEppn != null
                ? userService.getUiParams(authUserEppn)
                : Collections.emptyMap();
        return uiFetchMapper.toUiMeDto(
                user,
                userRoles,
                authUser,
                authUserRoles,
                authUserEppn != null ? userShareService.getSuUsers(authUserEppn) : Collections.emptyList(),
                user != null && user.getSignImagesIds() != null ? user.getSignImagesIds() : Collections.emptyList(),
                user != null ? user.getKeystoreFileName() : null,
                uiParams,
                httpSession != null ? httpSession.getAttribute("securityServiceName") : null
        );
    }

    public UiCountersDto buildUiCounters(String userEppn, String authUserEppn) {
        if (userEppn == null) {
            return uiFetchMapper.toUiCountersDto(0L, 0L, 0L, 0, 0, false, false, false, false);
        }
        Integer reportNumber = authUserEppn != null ? reportService.countByUser(authUserEppn) : 0;
        Integer managedWorkflowsSize = authUserEppn != null ? workflowService.getWorkflowByManagersContains(authUserEppn).size() : 0;
        Boolean isRoleManager = authUserEppn != null && preAuthorizeService.isManager(authUserEppn);
        Boolean isOneSignShare = authUserEppn != null && userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.sign);
        Boolean isOneReadShare = authUserEppn != null && userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.read);
        Boolean certificatProblem = userService.getByEppn(userEppn) != null && certificatService.checkCertificatProblem(userService.getRoles(userEppn));
        return uiFetchMapper.toUiCountersDto(
                signRequestService.getNbPendingSignRequests(userEppn),
                signBookService.nbToSignSignBooks(userEppn),
                signBookService.nbDeleted(userEppn),
                reportNumber,
                managedWorkflowsSize,
                isRoleManager,
                isOneSignShare,
                isOneReadShare,
                certificatProblem
        );
    }

    public UiConfigDto buildUiConfig(String userEppn, Integer maxInactiveInterval) {
        FrontendGlobalProperties frontendGlobalProperties = userEppn != null ? generateFrontendGlobalProperties(userEppn) : null;
        String profile = null;
        if (environment.getActiveProfiles().length > 0 && "dev".equals(environment.getActiveProfiles()[0])) {
            profile = environment.getActiveProfiles()[0];
        }
        String versionApp = buildProperties != null ? buildProperties.getVersion() : "dev";
        return uiFetchMapper.toUiConfigDto(
                frontendGlobalProperties,
                smsProperties.getEnableSms(),
                validationService != null,
                globalProperties.getApplicationEmail(),
                maxInactiveInterval,
                globalProperties.getHoursBeforeRefreshNotif(),
                globalProperties.getInfiniteScrolling(),
                versionApp,
                profile
        );
    }

    private FrontendGlobalProperties generateFrontendGlobalProperties(String userEppn) {
        GlobalProperties myGlobalProperties = new GlobalProperties();
        BeanUtils.copyProperties(globalProperties, myGlobalProperties);
        userService.parseRoles(userEppn, myGlobalProperties);
        myGlobalProperties.newVersion = globalProperties.newVersion;
        return FrontendGlobalProperties.fromGlobalProperties(myGlobalProperties);
    }
}


