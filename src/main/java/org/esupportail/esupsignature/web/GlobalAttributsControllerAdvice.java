package org.esupportail.esupsignature.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.ui.global.UiDataDto;
import org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto;
import org.esupportail.esupsignature.dto.ui.global.UiCountersDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserShareService;
import org.esupportail.esupsignature.dto.mapper.UiFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller"})
@EnableConfigurationProperties(GlobalProperties.class)
public class GlobalAttributsControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalAttributsControllerAdvice.class);

    private final GlobalProperties globalProperties;
    private final UserShareService userShareService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final ServletContext servletContext;
    private final UiFetchService uiFetchService;

    public GlobalAttributsControllerAdvice(GlobalProperties globalProperties, UserShareService userShareService, UserService userService, ObjectMapper objectMapper,
                                           ServletContext servletContext,
                                           UiFetchService uiFetchService) {
        this.globalProperties = globalProperties;
        this.userShareService = userShareService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.servletContext = servletContext;
        this.uiFetchService = uiFetchService;
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
        model.addAttribute("favoriteWorkflowIds", authUserEppn != null ? userService.getFavoriteIds(authUserEppn, UiParams.favoriteWorkflows) : java.util.List.of());
        model.addAttribute("favoriteFormIds", authUserEppn != null ? userService.getFavoriteIds(authUserEppn, UiParams.favoriteForms) : java.util.List.of());
        HttpSession httpSession = httpServletRequest.getSession();
        httpSession.setMaxInactiveInterval((int) sessionTimeout.toSeconds());
        if(userEppn != null) {
            User user = userService.getFullUserByEppn(userEppn);
            if(user == null) {
                logger.error("user {} not found", userEppn);
                return;
            }
            UiGlobalPropertiesDto myGlobalProperties = uiFetchService.buildUiGlobalProperties(userEppn);
            UiCountersDto uiCounters = uiFetchService.buildUiCounters(userEppn, authUserEppn);
            UiDataDto.UiConfigDto uiConfig = uiFetchService.buildUiConfig(userEppn, httpSession.getMaxInactiveInterval());
            model.addAttribute("user", user);
            model.addAttribute("authUser", userService.getByEppn(authUserEppn));
            model.addAttribute("keystoreFileName", user.getKeystoreFileName());
            model.addAttribute("userImagesIds", user.getSignImagesIds());
            model.addAttribute("suUsers", userShareService.getSuUsers(authUserEppn));
            model.addAttribute("isOneSignShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.sign));
            model.addAttribute("isOneReadShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.read));
            model.addAttribute("managedWorkflowsSize",  uiCounters.managedWorkflowsSize());
            model.addAttribute("isRoleManager", uiCounters.isRoleManager());
            model.addAttribute("infiniteScrolling", uiConfig.infiniteScrolling());
            model.addAttribute("validationToolsEnabled", uiConfig.validationToolsEnabled());
            model.addAttribute("globalProperties", myGlobalProperties);
            model.addAttribute("globalPropertiesJson", objectMapper.writer().writeValueAsString(myGlobalProperties));
            model.addAttribute("enableSms", uiConfig.enableSms());
            model.addAttribute("reportNumber", uiCounters.reportNumber());
            if (uiConfig.profile() != null) {
                model.addAttribute("profile", uiConfig.profile());
            }
            model.addAttribute("versionApp", uiConfig.versionApp());
            model.addAttribute("signTypes", Arrays.stream(SignType.values()).sorted(Comparator.comparingInt(SignType::getValue).reversed()).toList());
            model.addAttribute("signLevels", Arrays.stream(SignLevel.values()).sorted(Comparator.comparingInt(SignLevel::getValue)).toList());

            model.addAttribute("nbSignRequests", uiCounters.nbSignRequests());
            model.addAttribute("nbToSign", uiCounters.nbToSign());
            model.addAttribute("nbDeleted", uiCounters.nbDeleted());
            model.addAttribute("certificatProblem", uiCounters.certificatProblem());
        }
        model.addAttribute("maxInactiveInterval", httpSession.getMaxInactiveInterval());
        model.addAttribute("applicationEmail", globalProperties.getApplicationEmail());
    }


}
