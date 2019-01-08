package org.esupportail.esupsignature.web.manager;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.service.SignBookService;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping("/manager/signbooks")
@Controller
@RooWebScaffold(path = "manager/signbooks", formBackingObject = SignBook.class)
public class SignBookController {
	
	@Resource 
	SignBookService signBookService;
	
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid SignBook signBook, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, signBook);
            return "manager/signbooks/create";
        }
        uiModel.asMap().clear();
        signBookService.create(signBook);
        return "redirect:/manager/signbooks/" + encodeUrlPathSegment(signBook.getId().toString(), httpServletRequest);
    }
	
}
