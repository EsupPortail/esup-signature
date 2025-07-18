package org.esupportail.esupsignature.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller", "org.esupportail.esupsignature.web.otp"})
@EnableConfigurationProperties(GlobalProperties.class)
public class GlobalAttributsControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalAttributsControllerAdvice.class);

    private final GlobalProperties globalProperties;

    private final SmsProperties smsProperties;

    private final SignRequestService signRequestService;

    private final SignBookService signBookService;

    private final WorkflowService workflowService;

    private final UserShareService userShareService;

    private final UserService userService;

    private final ReportService reportService;

    private final SignTypeService signTypeService;

    private final PreAuthorizeService preAuthorizeService;

    private final ObjectMapper objectMapper;

    private final Environment environment;

    private final BuildProperties buildProperties;

    private final ValidationService validationService;

    private final CertificatService certificatService;

    private final ServletContext servletContext;

    public GlobalAttributsControllerAdvice(GlobalProperties globalProperties, SmsProperties smsProperties, SignRequestService signRequestService, SignBookService signBookService, WorkflowService workflowService, UserShareService userShareService, UserService userService, ReportService reportService, SignTypeService signTypeService, PreAuthorizeService preAuthorizeService, ObjectMapper objectMapper,
                                           @Autowired(required = false) BuildProperties buildProperties,
                                           ValidationService validationService,
                                           Environment environment, CertificatService certificatService, ServletContext servletContext) {
        this.globalProperties = globalProperties;
        this.smsProperties = smsProperties;
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.workflowService = workflowService;
        this.userShareService = userShareService;
        this.userService = userService;
        this.reportService = reportService;
        this.signTypeService = signTypeService;
        this.preAuthorizeService = preAuthorizeService;
        this.objectMapper = objectMapper;
        this.buildProperties = buildProperties;
        this.validationService = validationService;
        this.environment = environment;
        this.certificatService = certificatService;
        this.servletContext = servletContext;
    }

    @Value("${spring.session.timeout:#{null}}")
    private Duration sessionTimeout;

    @PostConstruct
    public void init() {
        if (sessionTimeout == null) {
            int defaultTimeoutMinutes = servletContext.getSessionTimeout();
            sessionTimeout = Duration.ofMinutes(defaultTimeoutMinutes);
        }
        logger.info("Session timeout = " + sessionTimeout.toMinutes() + " min");
    }

    @ModelAttribute
    public void globalAttributes(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model, HttpServletRequest httpServletRequest) throws JsonProcessingException {
        model.addAttribute("currentUri", httpServletRequest.getRequestURI());
        HttpSession httpSession = httpServletRequest.getSession();
        httpSession.setMaxInactiveInterval((int) sessionTimeout.toSeconds());
        if(userEppn != null) {
            GlobalProperties myGlobalProperties = new GlobalProperties();
            BeanUtils.copyProperties(globalProperties, myGlobalProperties);
            User user = userService.getFullUserByEppn(userEppn);
            if(user == null) {
                logger.error("user " + userEppn + " not found");
                return;
            }
            List<String> roles = userService.getRoles(userEppn);
            userService.parseRoles(userEppn, myGlobalProperties);
            model.addAttribute("securityServiceName", httpServletRequest.getSession().getAttribute("securityServiceName"));
            model.addAttribute("user", user);
            model.addAttribute("authUser", userService.getByEppn(authUserEppn));
            model.addAttribute("keystoreFileName", user.getKeystoreFileName());
            model.addAttribute("userImagesIds", user.getSignImagesIds());
            model.addAttribute("suUsers", userShareService.getSuUsers(authUserEppn));
            model.addAttribute("isOneSignShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.sign));
            model.addAttribute("isOneReadShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.read));
            model.addAttribute("managedWorkflowsSize",  workflowService.getWorkflowByManagersContains(authUserEppn).size());
            model.addAttribute("isManager", preAuthorizeService.isManager(authUserEppn));
            model.addAttribute("infiniteScrolling", globalProperties.getInfiniteScrolling());
            model.addAttribute("validationToolsEnabled", validationService != null);
            model.addAttribute("globalProperties", myGlobalProperties);
            model.addAttribute("globalPropertiesJson", objectMapper.writer().writeValueAsString(myGlobalProperties));
            model.addAttribute("enableSms", smsProperties.getEnableSms());
            model.addAttribute("reportNumber", reportService.countByUser(authUserEppn));
            model.addAttribute("hoursBeforeRefreshNotif", myGlobalProperties.getHoursBeforeRefreshNotif());
            model.addAttribute("myUiParams", userService.getUiParams(authUserEppn));
            if (environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0].equals("dev")) {
                model.addAttribute("profile", environment.getActiveProfiles()[0]);
            }
            if (buildProperties != null) {
                model.addAttribute("versionApp", buildProperties.getVersion());
            } else {
                model.addAttribute("versionApp", "dev");
            }
            model.addAttribute("signTypes", Arrays.stream(SignType.values()).sorted(Comparator.comparingInt(SignType::getValue).reversed()).toList());
            model.addAttribute("signLevels", Arrays.stream(SignLevel.values()).sorted(Comparator.comparingInt(SignLevel::getValue)).toList());

            model.addAttribute("nbSignRequests", signRequestService.getNbPendingSignRequests(userEppn));
            model.addAttribute("nbToSign", signBookService.nbToSignSignBooks(userEppn));
            model.addAttribute("certificatProblem", certificatService.checkCertificatProblem(roles));
        }
        model.addAttribute("maxInactiveInterval", httpSession.getMaxInactiveInterval());
        model.addAttribute("applicationEmail", globalProperties.getApplicationEmail());
    }

}
