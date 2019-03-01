// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.web.manager;

import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.web.manager.SignBookController;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

privileged aspect SignBookController_Roo_Controller {
    
    @RequestMapping(params = "form", produces = "text/html")
    public String SignBookController.createForm(Model uiModel) {
        populateEditForm(uiModel, new SignBook());
        return "manager/signbooks/create";
    }
    
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String SignBookController.update(@Valid SignBook signBook, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, signBook);
            return "manager/signbooks/update";
        }
        uiModel.asMap().clear();
        signBook.merge();
        return "redirect:/manager/signbooks/" + encodeUrlPathSegment(signBook.getId().toString(), httpServletRequest);
    }
    
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String SignBookController.updateForm(@PathVariable("id") Long id, Model uiModel) {
        populateEditForm(uiModel, SignBook.findSignBook(id));
        return "manager/signbooks/update";
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String SignBookController.delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
        SignBook signBook = SignBook.findSignBook(id);
        signBook.remove();
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/manager/signbooks";
    }
    
    void SignBookController.addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("signBook_createdate_date_format", "dd/MM/yyyy HH:mm");
        uiModel.addAttribute("signBook_updatedate_date_format", "dd/MM/yyyy HH:mm");
    }
    
    String SignBookController.encodeUrlPathSegment(String pathSegment, HttpServletRequest httpServletRequest) {
        String enc = httpServletRequest.getCharacterEncoding();
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        pathSegment = UriUtils.encodePathSegment(pathSegment, enc);
        return pathSegment;
    }
    
}
