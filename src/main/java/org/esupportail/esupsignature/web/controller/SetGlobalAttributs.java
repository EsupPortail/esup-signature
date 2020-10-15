package org.esupportail.esupsignature.web.controller;

import org.apache.commons.beanutils.BeanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserShareService;
import org.esupportail.esupsignature.service.file.FileService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller.user", "org.esupportail.esupsignature.web.controller.admin"}, basePackageClasses = {IndexController.class})
public class SetGlobalAttributs {

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private UserService userService;

    @Resource
    private FileService fileService;
    
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
    public void globalAttributes(@ModelAttribute(name = "user") User user, @ModelAttribute(name = "authUser") User authUser, Model model) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException  {
        List<Message> messages = new ArrayList<>();
        if(authUser != null) {
            this.myGlobalProperties = (GlobalProperties) BeanUtils.cloneBean(globalProperties);
            if ((authUser.getSplash() == null || !authUser.getSplash()) && globalProperties.getEnableSplash() && !authUser.getEppn().equals("system")) {
                Message splashMessage = new Message();
                splashMessage.setText(fileService.readFileToString("/templates/splash.html"));
                splashMessage.setId(0L);
                messages.add(splashMessage);
            } else if (!authUser.getEppn().equals("system") && user.equals(authUser)) {
                messages.addAll(userService.getMessages(authUser));
            }
            model.addAttribute("messageNews", messages);
            parseRoles(user);
            model.addAttribute("suUsers", userShareService.getSuUsers(authUser));
            model.addAttribute("isOneCreateShare", userShareService.isOneShareByType(user, authUser, ShareType.create));
            model.addAttribute("isOneSignShare", userShareService.isOneShareByType(user, authUser, ShareType.sign));
            model.addAttribute("isOneReadShare", userShareService.isOneShareByType(user, authUser, ShareType.read));
        }
        model.addAttribute("globalProperties", this.myGlobalProperties);
    }

    private void parseRoles(User user) {
        if(user.getRoles().contains("create_signrequest")) {
            this.myGlobalProperties.setHideSendSignRequest("false");
        }
        if(user.getRoles().contains("create_wizard")) {
            this.myGlobalProperties.setHideWizard("false");
        }
        if(user.getRoles().contains("create_autosign")) {
            this.myGlobalProperties.setHideAutoSign("false");
        }
        if(user.getRoles().contains("no_create_signrequest")) {
            this.myGlobalProperties.setHideSendSignRequest("true");
        }
        if(user.getRoles().contains("no_create_wizard")) {
            this.myGlobalProperties.setHideWizard("true");
        }
        if(user.getRoles().contains("no_create_autosign")) {
            this.myGlobalProperties.setHideAutoSign("true");
        }
    }

}
