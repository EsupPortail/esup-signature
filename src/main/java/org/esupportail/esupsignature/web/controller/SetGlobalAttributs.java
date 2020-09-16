package org.esupportail.esupsignature.web.controller;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice()
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

    @ModelAttribute
    public void globalAttributes(@ModelAttribute(name = "user") User user, @ModelAttribute(name = "authUser") User authUser, Model model) {
        List<Message> messages = new ArrayList<>();
        if(!authUser.getEppn().equals("system") && user.equals(authUser)) {
            messages.addAll(userService.getMessages(authUser));
        }
        model.addAttribute("suUsers", userService.getSuUsers(authUser));
        model.addAttribute("globalProperties", this.globalProperties);
        model.addAttribute("messageNews", messages);
    }

}
