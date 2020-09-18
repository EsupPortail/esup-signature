package org.esupportail.esupsignature.web.controller;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.file.FileService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
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

    @ModelAttribute(value = "user", binding = false)
    public User getUser() {
        return userService.getCurrentUser();
    }

    @ModelAttribute(value = "authUser", binding = false)
    public User getAuthUser() {
        return userService.getUserFromAuthentication();
    }

    @ModelAttribute
    public void globalAttributes(@ModelAttribute(name = "user") User user, @ModelAttribute(name = "authUser") User authUser, Model model) {
        List<Message> messages = new ArrayList<>();
        if((authUser.getSplash() == null || !authUser.getSplash()) && globalProperties.getEnableSplash() && !authUser.getEppn().equals("system")) {
            Message splashMessage = new Message();
            splashMessage.setText(fileService.readFileToString("/templates/splash.html"));
            splashMessage.setId(0L);
            messages.add(splashMessage);
        } else if(!authUser.getEppn().equals("system") && user.equals(authUser)) {
            messages.addAll(userService.getMessages(authUser));
        }
        model.addAttribute("messageNews", messages);
        model.addAttribute("suUsers", userService.getSuUsers(authUser));
        model.addAttribute("globalProperties", this.globalProperties);
        model.addAttribute("isOneCreateShare", userService.checkOneServiceShare(user, authUser, UserShare.ShareType.create));
        model.addAttribute("isOneSignShare", userService.checkOneServiceShare(user, authUser, UserShare.ShareType.sign));
    }

}
