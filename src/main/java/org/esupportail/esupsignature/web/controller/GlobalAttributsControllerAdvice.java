package org.esupportail.esupsignature.web.controller;

import org.apache.commons.beanutils.BeanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller"})
public class GlobalAttributsControllerAdvice {

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
    public void globalAttributes(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
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
        model.addAttribute("formManaged", formService.getFormByManagersContains(authUser.getEmail()));
        model.addAttribute("validationToolsEnabled", validationService != null);
        model.addAttribute("globalProperties", this.myGlobalProperties);
        if (buildProperties != null) {
            model.addAttribute("version", buildProperties.getVersion());
        }
        List<SignType> signTypes = Arrays.asList(SignType.values());
        if(userKeystoreService == null) {
        	signTypes.remove(SignType.certSign);
        	signTypes.remove(SignType.nexuSign);
        }
        model.addAttribute("signTypes", signTypes);
        model.addAttribute("nbDatas", dataService.getNbCreateByAndStatus(userEppn));
        model.addAttribute("nbSignRequests", signRequestService.getNbByCreateAndStatus(userEppn));
        model.addAttribute("nbToSign", signRequestService.nbToSignRequests(userEppn));
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