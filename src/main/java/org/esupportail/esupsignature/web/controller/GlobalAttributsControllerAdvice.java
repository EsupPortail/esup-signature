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
    public void globalAttributes(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, Model model) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        User user = userService.getById(userId);
        model.addAttribute("user", user);
        parseRoles(user);
        User authUser = userService.getById(authUserId);
        model.addAttribute("authUser", authUser);
        this.myGlobalProperties = (GlobalProperties) BeanUtils.cloneBean(globalProperties);
        model.addAttribute("keystoreFileName", userService.getKeystoreFileName(authUserId));
        model.addAttribute("userImagesIds", userService.getSignImagesIds(authUserId));
        model.addAttribute("suUsers", userShareService.getSuUsers(authUserId));
        model.addAttribute("isOneCreateShare", userShareService.isOneShareByType(userId, authUserId, ShareType.create));
        model.addAttribute("isOneSignShare", userShareService.isOneShareByType(userId, authUserId, ShareType.sign));
        model.addAttribute("isOneReadShare", userShareService.isOneShareByType(userId, authUserId, ShareType.read));
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
        model.addAttribute("nbDatas", dataService.getNbCreateByAndStatus(user.getEppn()));
        model.addAttribute("nbSignRequests", signRequestService.getNbByCreateAndStatus(userId));
        model.addAttribute("nbToSign", signRequestService.getToSignRequests(userId).size());
        model.addAttribute("forms", formService.getFormsByUser(userId, authUserId));
    }

    public void parseRoles(User user) {
        if (!Collections.disjoint(user.getRoles(), globalProperties.getHideSendSignExceptRoles()))
            myGlobalProperties.setHideSendSignRequest(!globalProperties.getHideSendSignRequest());
        if (!Collections.disjoint(user.getRoles(), globalProperties.getHideWizardExceptRoles()))
            myGlobalProperties.setHideWizard(!globalProperties.getHideWizard());
        if (!Collections.disjoint(user.getRoles(), globalProperties.getHideAutoSignExceptRoles()))
            myGlobalProperties.setHideAutoSign(!globalProperties.getHideAutoSign());

        if (user.getRoles().contains("create_signrequest")) {
            myGlobalProperties.setHideSendSignRequest(false);
        }
        if (user.getRoles().contains("create_wizard")) {
            myGlobalProperties.setHideWizard(false);
        }
        if (user.getRoles().contains("create_autosign")) {
            myGlobalProperties.setHideAutoSign(false);
        }

        if (user.getRoles().contains("no_create_signrequest")) {
            myGlobalProperties.setHideSendSignRequest(true);
        }
        if (user.getRoles().contains("no_create_wizard")) {
            myGlobalProperties.setHideWizard(true);
        }
        if (user.getRoles().contains("no_create_autosign")) {
            myGlobalProperties.setHideAutoSign(true);
        }
    }
}