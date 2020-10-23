package org.esupportail.esupsignature.web.controller;

import org.apache.commons.beanutils.BeanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserShareService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller"})
public class SetGlobalAttributs {

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private UserService userService;

    @Resource
    private FormRepository formRepository;

    @Resource
    private UserShareService userShareService;

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
    public void globalAttributes(@ModelAttribute(name = "user") User user, @ModelAttribute(name = "authUser") User authUser, Model model) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if(authUser != null) {
            this.myGlobalProperties = (GlobalProperties) BeanUtils.cloneBean(globalProperties);
            parseRoles(user);
            model.addAttribute("suUsers", userShareService.getSuUsers(authUser));
            model.addAttribute("isOneCreateShare", userShareService.isOneShareByType(user, authUser, ShareType.create));
            model.addAttribute("isOneSignShare", userShareService.isOneShareByType(user, authUser, ShareType.sign));
            model.addAttribute("isOneReadShare", userShareService.isOneShareByType(user, authUser, ShareType.read));
            model.addAttribute("formManaged", formRepository.findFormByManagersContains(authUser.getEmail()));
        }
        model.addAttribute("globalProperties", this.myGlobalProperties);
    }

    private void parseRoles(User user) {
        if(!Collections.disjoint(user.getRoles(), globalProperties.getHideSendSignExceptRoles())) myGlobalProperties.setHideSendSignRequest(!globalProperties.getHideSendSignRequest());
        if(!Collections.disjoint(user.getRoles(), globalProperties.getHideWizardExceptRoles())) myGlobalProperties.setHideWizard(!globalProperties.getHideWizard());
        if(!Collections.disjoint(user.getRoles(), globalProperties.getHideAutoSignExceptRoles())) myGlobalProperties.setHideAutoSign(!globalProperties.getHideAutoSign());

        if(user.getRoles().contains("create_signrequest")) {
            myGlobalProperties.setHideSendSignRequest(false);
        }
        if(user.getRoles().contains("create_wizard")) {
            myGlobalProperties.setHideWizard(false);
        }
        if(user.getRoles().contains("create_autosign")) {
            myGlobalProperties.setHideAutoSign(false);
        }

        if(user.getRoles().contains("no_create_signrequest")) {
            myGlobalProperties.setHideSendSignRequest(true);
        }
        if(user.getRoles().contains("no_create_wizard")) {
            myGlobalProperties.setHideWizard(true);
        }
        if(user.getRoles().contains("no_create_autosign")) {
            myGlobalProperties.setHideAutoSign(true);
        }
    }
}
