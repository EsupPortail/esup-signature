package org.esupportail.esupsignature.web.manager;
import org.esupportail.esupsignature.domain.SignBook;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/manager/signbooks")
@Controller
@RooWebScaffold(path = "signbooks", formBackingObject = SignBook.class)
public class SignBookController {
}
