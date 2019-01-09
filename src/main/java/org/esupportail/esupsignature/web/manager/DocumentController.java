package org.esupportail.esupsignature.web.manager;
import java.io.IOException;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/manager/documents")
@Controller
@RooWebScaffold(path = "manager/documents", formBackingObject = Document.class)
public class DocumentController {
	
	private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

	@Resource
	FileService fileService;
	
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid Document document, @RequestParam("multipartFile") MultipartFile multipartFile, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, document);
            return "manager/documents/create";
        }
        uiModel.asMap().clear();
        try {
			document.setOriginalFile(fileService.addFile(multipartFile));
			document.setSignedFile(null);
	        document.persist();
        } catch (IOException | SQLException e) {
        	log.error("Create file error", e);
		}
        return "redirect:/manager/documents/" + encodeUrlPathSegment(document.getId().toString(), httpServletRequest);
    }
}
