package org.esupportail.esupsignature.web.manager;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/manager/logs")
@Controller
@RooWebScaffold(path = "manager/logs", create = false, delete = false, update = false, formBackingObject = Log.class)
public class LogController {

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "manager/logs";
	}
	
	@Resource
	private UserService userService;
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}
}
