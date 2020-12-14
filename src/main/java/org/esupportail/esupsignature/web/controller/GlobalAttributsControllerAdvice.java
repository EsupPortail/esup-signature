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
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller"})
public class GlobalAttributsControllerAdvice {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private UserService userService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private FormService formService;

    @Resource
    private DataService dataService;

    @Resource
    private UserShareService userShareService;

    @Autowired(required = false)
    private ValidationService validationService;
    
    @Autowired(required = false)
    private UserKeystoreService userKeystoreService;

    private GlobalProperties myGlobalProperties;

    @ModelAttribute
    public void globalAttributes(@ModelAttribute(name = "user") User user, @ModelAttribute(name = "authUser") User authUser, HttpServletRequest request, Model model) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String method = request.getMethod();
        if (!method.equals("GET")) return;
        if (authUser != null) {
            this.myGlobalProperties = (GlobalProperties) BeanUtils.cloneBean(globalProperties);
            parseRoles(user);
            model.addAttribute("suUsers", userShareService.getSuUsers(authUser));
            model.addAttribute("isOneCreateShare", userShareService.isOneShareByType(user, authUser, ShareType.create));
            model.addAttribute("isOneSignShare", userShareService.isOneShareByType(user, authUser, ShareType.sign));
            model.addAttribute("isOneReadShare", userShareService.isOneShareByType(user, authUser, ShareType.read));
            model.addAttribute("formManaged", formService.getFormByManagersContains(authUser));
            model.addAttribute("validationToolsEnabled", validationService != null);
        }
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

        if (user != null) {
            model.addAttribute("nbDatas", dataService.getNbCreateByAndStatus(user));
            model.addAttribute("nbSignRequests", signRequestService.getNbByCreateAndStatus(user));
            model.addAttribute("nbToSign", signRequestService.getToSignRequests(user).size());
        }
    }

    private void parseRoles(User user) {
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
