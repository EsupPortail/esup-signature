package org.esupportail.esupsignature.web.controller;

import org.apache.commons.beanutils.BeanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller"})
public class SetGlobalAttributs {

    private static final Logger logger = LoggerFactory.getLogger(SetGlobalAttributs.class);

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private UserService userService;
    @Resource
    private SignRequestService signRequestService;

    @Resource
    private FormRepository formRepository;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private DataRepository dataRepository;

    @Resource
    private UserShareService userShareService;

    @Autowired(required = false)
    private ValidationService validationService;
    
    @Autowired(required = false)
    private UserKeystoreService userKeystoreService;

    private GlobalProperties myGlobalProperties;

    @ModelAttribute(value = "user", binding = false)
    public User getUser() {
        return userService.getCurrentUser();
    }

    @ModelAttribute(value = "authUser", binding = false)
    public User getAuthUser() {
        return userService.getUserFromAuthentication();
    }


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
            model.addAttribute("formManaged", formRepository.findFormByManagersContains(authUser.getEmail()));
            model.addAttribute("validationToolsEnabled", validationService != null);
        }
        model.addAttribute("globalProperties", this.myGlobalProperties);
        if (buildProperties != null) {
            model.addAttribute("version", buildProperties.getVersion());
        }
        List<SignType> signTypes = new ArrayList(Arrays.asList(SignType.values()));
        if(userKeystoreService == null) {
        	signTypes.remove(SignType.certSign);
        	signTypes.remove(SignType.nexuSign);
        }
        model.addAttribute("signTypes", signTypes);

        if (user != null) {
            List<String> base64UserSignatures = userService.getBase64UserSignatures(user);
            model.addAttribute("base64UserSignatures", base64UserSignatures);
            model.addAttribute("nbDatas", dataRepository.findByCreateByAndStatus(user.getEppn(), SignRequestStatus.draft).size());
            model.addAttribute("nbSignRequests", signRequestRepository.findByCreateByAndStatus(user, SignRequestStatus.pending).size());
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
