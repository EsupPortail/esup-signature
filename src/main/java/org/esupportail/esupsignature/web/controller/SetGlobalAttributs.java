package org.esupportail.esupsignature.web.controller;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import java.util.List;

@ControllerAdvice
public class SetGlobalAttributs {

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private UserService userService;

    @ModelAttribute(value = "user", binding = false)
    public User getUser() {
        return userService.getCurrentUser();
    }

    @ModelAttribute(value = "authUser", binding = false)
    public User getAuthUser() {
        return userService.getUserFromAuthentication();
    }

    @ModelAttribute(value = "suUsers", binding = false)
    public List<User> getSuUsers() {
        return userService.getSuUsers(userService.getUserFromAuthentication());
    }

    @ModelAttribute(value = "globalProperties")
    public GlobalProperties getGlobalProperties() {
        return this.globalProperties;
    }

    @ModelAttribute(value = "messageNews", binding = false)
    public List<Message> getMessageNews() {
        return userService.getMessages(getAuthUser());
    }

}
