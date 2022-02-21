package org.esupportail.esupsignature.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.OJService;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller", "org.esupportail.esupsignature.web.otp"})
@EnableConfigurationProperties(GlobalProperties.class)
public class GlobalAttributsControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalAttributsControllerAdvice.class);

    private final GlobalProperties globalProperties;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private FormService formService;

    @Resource
    private UserShareService userShareService;

    @Resource
    private UserService userService;

    @Resource
    private ReportService reportService;

    @Resource
    private SignTypeService signTypeService;

    @Resource
    private OJService ojService;

    private final Environment environment;

    private final BuildProperties buildProperties;

    private final ValidationService validationService;

    private final UserKeystoreService userKeystoreService;

    public GlobalAttributsControllerAdvice(GlobalProperties globalProperties,
                                           @Autowired(required = false) BuildProperties buildProperties,
                                           ValidationService validationService,
                                           UserKeystoreService userKeystoreService,
                                           Environment environment) {
        this.globalProperties = globalProperties;
        this.buildProperties = buildProperties;
        this.validationService = validationService;
        this.userKeystoreService = userKeystoreService;
        this.environment = environment;
    }

    @ModelAttribute
    public void globalAttributes(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model, HttpServletRequest httpServletRequest) throws JsonProcessingException {
        if(userEppn != null) {
            GlobalProperties myGlobalProperties = new GlobalProperties();
            BeanUtils.copyProperties(globalProperties, myGlobalProperties);
            User user = userService.getUserByEppn(userEppn);
            parseRoles(userEppn, myGlobalProperties);
            model.addAttribute("user", user);
            model.addAttribute("authUser", userService.getUserByEppn(authUserEppn));
            model.addAttribute("keystoreFileName", user.getKeystoreFileName());
            model.addAttribute("userImagesIds", user.getSignImagesIds());
            model.addAttribute("suUsers", userShareService.getSuUsers(authUserEppn));
            model.addAttribute("isOneCreateShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.create));
            model.addAttribute("isOneSignShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.sign));
            model.addAttribute("isOneReadShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.read));
            model.addAttribute("managedFormsSize", formService.getFormByManagersContains(authUserEppn).size());
            model.addAttribute("infiniteScrolling", globalProperties.getInfiniteScrolling());
            model.addAttribute("validationToolsEnabled", validationService != null);
            model.addAttribute("globalProperties", myGlobalProperties);
            ObjectMapper objectMapper = new ObjectMapper();
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
            List<SignType> signTypes = signTypeService.getAuthorizedSignTypes();
            if (userKeystoreService == null) {
                signTypes.remove(SignType.certSign);
                signTypes.remove(SignType.nexuSign);
            }
            model.addAttribute("signTypes", signTypes);
            model.addAttribute("nbSignRequests", signRequestService.getNbPendingSignRequests(userEppn));
            model.addAttribute("nbDraft", signRequestService.getNbDraftSignRequests(userEppn));
            model.addAttribute("nbToSign", signRequestService.nbToSignSignRequests(userEppn));
            try {
                model.addAttribute("dssStatus", ojService.checkOjFreshness());
            } catch (IOException e) {
                logger.debug("enable to get dss status");
            }
        }
        model.addAttribute("applicationEmail", globalProperties.getApplicationEmail());
    }

    @Transactional
    public void parseRoles(String userEppn, GlobalProperties myGlobalProperties) {
        User user = userService.getByEppn(userEppn);
        List<String> roles = user.getRoles();
        if (!Collections.disjoint(roles, globalProperties.getHideSendSignExceptRoles()))
            myGlobalProperties.setHideSendSignRequest(!globalProperties.getHideSendSignRequest());
        if (!Collections.disjoint(roles, globalProperties.getHideWizardExceptRoles()))
            myGlobalProperties.setHideWizard(!globalProperties.getHideWizard());
        if (!Collections.disjoint(roles, globalProperties.getHideAutoSignExceptRoles()))
            myGlobalProperties.setHideAutoSign(!globalProperties.getHideAutoSign());

        if (roles.contains("ROLE_CREATE_SIGNREQUEST")) {
            myGlobalProperties.setHideSendSignRequest(false);
        }
        if (roles.contains("ROLE_CREATE_WIZARD")) {
            myGlobalProperties.setHideWizard(false);
        }
        if (roles.contains("ROLE_CREATE_AUTOSIGN")) {
            myGlobalProperties.setHideAutoSign(false);
        }
        if (roles.contains("ROLE_NO_CREATE_SIGNREQUEST")) {
            myGlobalProperties.setHideSendSignRequest(true);
        }
        if (roles.contains("ROLE_NO_CREATE_WIZARD")) {
            myGlobalProperties.setHideWizard(true);
        }
        if (roles.contains("ROLE_NO_CREATE_AUTOSIGN")) {
            myGlobalProperties.setHideAutoSign(true);
        }
    }
}
