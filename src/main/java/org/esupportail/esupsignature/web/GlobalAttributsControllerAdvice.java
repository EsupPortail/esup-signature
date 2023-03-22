package org.esupportail.esupsignature.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.OJService;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller", "org.esupportail.esupsignature.web.otp"})
@EnableConfigurationProperties(GlobalProperties.class)
public class GlobalAttributsControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalAttributsControllerAdvice.class);

    private final GlobalProperties globalProperties;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private UserShareService userShareService;

    @Resource
    private UserService userService;

    @Resource
    private ReportService reportService;

    @Resource
    private SignTypeService signTypeService;

    @Resource
    private PreAuthorizeService preAuthorizeService;

    @Resource
    private OJService ojService;

    @Resource
    private ObjectMapper objectMapper;

    private final Environment environment;

    private final BuildProperties buildProperties;

    private final ValidationService validationService;

    public GlobalAttributsControllerAdvice(GlobalProperties globalProperties,
                                           @Autowired(required = false) BuildProperties buildProperties,
                                           ValidationService validationService,
                                           Environment environment) {
        this.globalProperties = globalProperties;
        this.buildProperties = buildProperties;
        this.validationService = validationService;
        this.environment = environment;
    }

    @ModelAttribute
    public void globalAttributes(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model, HttpServletRequest httpServletRequest) throws JsonProcessingException {
        HttpSession httpSession = httpServletRequest.getSession();
        if(userEppn != null) {
            GlobalProperties myGlobalProperties = new GlobalProperties();
            BeanUtils.copyProperties(globalProperties, myGlobalProperties);
            User user = userService.getFullUserByEppn(userEppn);
            userService.parseRoles(userEppn, myGlobalProperties);
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
            model.addAttribute("reportNumber", reportService.countByUser(authUserEppn));
            model.addAttribute("hoursBeforeRefreshNotif", myGlobalProperties.getHoursBeforeRefreshNotif());
            if (environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0].equals("dev")) {
                model.addAttribute("profile", environment.getActiveProfiles()[0]);
            }
            if (buildProperties != null) {
                model.addAttribute("versionApp", buildProperties.getVersion());
            } else {
                model.addAttribute("versionApp", "dev");
            }
            model.addAttribute("signTypes", signTypeService.getAuthorizedSignTypes());
            model.addAttribute("nbSignRequests", signRequestService.getNbPendingSignRequests(userEppn));
            model.addAttribute("nbDraft", signRequestService.getNbDraftSignRequests(userEppn));
            model.addAttribute("nbToSign", signRequestService.nbToSignSignRequests(userEppn));
            model.addAttribute("nbFollowByMe", signRequestService.nbFollowedByMe(userEppn));
            try {
                model.addAttribute("dssStatus", ojService.checkOjFreshness());
            } catch (IOException e) {
                logger.debug("enable to get dss status");
            }
        }
        model.addAttribute("applicationEmail", globalProperties.getApplicationEmail());
        model.addAttribute("maxInactiveInterval", httpSession.getMaxInactiveInterval());
    }

}
