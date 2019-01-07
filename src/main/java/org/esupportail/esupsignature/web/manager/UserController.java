package org.esupportail.esupsignature.web.manager;
import org.esupportail.esupsignature.domain.User;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/manager/users")
@Controller
@RooWebScaffold(path = "manager/users", formBackingObject = User.class)
public class UserController {
}
