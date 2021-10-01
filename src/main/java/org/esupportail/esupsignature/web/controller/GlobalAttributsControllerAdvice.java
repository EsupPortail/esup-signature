package org.esupportail.esupsignature.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.beanutils.BeanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.OJService;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller"})
public class GlobalAttributsControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalAttributsControllerAdvice.class);

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private FormService formService;

    @Resource
    private DataService dataService;

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

    @Autowired
    private Environment environment;

    private final BuildProperties buildProperties;

    private final ValidationService validationService;

    private final UserKeystoreService userKeystoreService;

    private GlobalProperties myGlobalProperties;

    public GlobalAttributsControllerAdvice(@Autowired(required = false) BuildProperties buildProperties,
                                           @Autowired(required = false) ValidationService validationService,
                                           @Autowired(required = false) UserKeystoreService userKeystoreService) {
        this.buildProperties = buildProperties;
        this.validationService = validationService;
        this.userKeystoreService = userKeystoreService;
    }

    @ModelAttribute
    public void globalAttributes(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, JsonProcessingException {
        this.myGlobalProperties = (GlobalProperties) BeanUtils.cloneBean(globalProperties);
        User user = userService.getUserByEppn(userEppn);
        model.addAttribute("user", user);
        parseRoles(user);
        User authUser = userService.getByEppn(authUserEppn);
        model.addAttribute("authUser", authUser);
        model.addAttribute("keystoreFileName", user.getKeystoreFileName());
        model.addAttribute("userImagesIds", user.getSignImagesIds());
        model.addAttribute("suUsers", userShareService.getSuUsers(authUserEppn));
        model.addAttribute("isOneCreateShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.create));
        model.addAttribute("isOneSignShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.sign));
        model.addAttribute("isOneReadShare", userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.read));
        model.addAttribute("managedForms", formService.getFormByManagersContains(authUserEppn));
        model.addAttribute("infiniteScrolling", globalProperties.getInfiniteScrolling());
        model.addAttribute("validationToolsEnabled", validationService != null);
        if(this.myGlobalProperties.getVersion().isEmpty()) this.myGlobalProperties.setVersion("dev");
        model.addAttribute("globalProperties", this.myGlobalProperties);
        ObjectMapper objectMapper = new ObjectMapper();
        model.addAttribute("globalPropertiesJson", objectMapper.writer().writeValueAsString(this.myGlobalProperties));
        model.addAttribute("reportNumber", reportService.countByUser(authUserEppn));
        model.addAttribute("hoursBeforeRefreshNotif", this.myGlobalProperties.getHoursBeforeRefreshNotif());
        if(environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0].equals("dev")) {
            model.addAttribute("profile", environment.getActiveProfiles()[0]);
        }
        if (buildProperties != null) {
            model.addAttribute("version", buildProperties.getVersion());
        } else {
            model.addAttribute("version", "dev");
        }
        List<SignType> signTypes = signTypeService.getAuthorizedSignTypes();
        if(userKeystoreService == null) {
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

    public void parseRoles(User user) {
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