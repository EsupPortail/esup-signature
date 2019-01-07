package org.esupportail.esupsignature.web.manager;
import org.esupportail.esupsignature.domain.Document;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/manager/documents")
@Controller
@RooWebScaffold(path = "manager/documents", formBackingObject = Document.class)
public class DocumentController {
}
