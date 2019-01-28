package org.esupportail.esupsignature.web.manager;
import java.util.Arrays;

import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.NewPageType;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignBook.SignType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/manager/signbooks")
@Controller
@RooWebScaffold(path = "manager/signbooks", formBackingObject = SignBook.class)
public class SignBookController {

    void populateEditForm(Model uiModel, SignBook signBook) {
        uiModel.addAttribute("signBook", signBook);
        uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
        uiModel.addAttribute("signBookTypes", Arrays.asList(SignBookType.values()));
        uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
        uiModel.addAttribute("newPageTypes", Arrays.asList(NewPageType.values()));        
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("signrequests", SignRequest.findAllSignRequests());
    }
    
}
