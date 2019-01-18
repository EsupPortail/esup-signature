package org.esupportail.esupsignature.web.manager;
import java.io.IOException;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.PdfService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/manager/signrequests")
@Controller
@RooWebScaffold(path = "manager/signrequests", formBackingObject = SignRequest.class)
@Transactional
@Scope(value="session")
public class SignRequestController {
	
	private static final Logger log = LoggerFactory.getLogger(SignRequestController.class);
	
	@Resource
	DocumentService documentService;

	@Resource
	PdfService pdfService;
	
	@Resource
	UserKeystoreService userKeystoreService;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "signrequests";
	}
	
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid SignRequest document, @RequestParam("multipartFile") MultipartFile multipartFile, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, document);
            return "manager/signrequests/create";
        }
        uiModel.asMap().clear();
        try {
        	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    		String eppn = auth.getName();
        	document.setCreateBy(eppn);
        	document.setCreateDate(new Date());
			document.setOriginalFile(documentService.addFile(multipartFile));
			document.setSignedFile(null);
			document.setStatus(SignRequestStatus.pending);
	        document.persist();
        } catch (IOException e) {
        	log.error("Create file error", e);
		}
        return "redirect:/manager/signrequests/" + encodeUrlPathSegment(document.getId().toString(), httpServletRequest);
    }
	
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        addDateTimeFormatPatterns(uiModel);
        SignRequest document = SignRequest.findSignRequest(id);
        uiModel.addAttribute("document", document);
        uiModel.addAttribute("itemId", id);
        return "manager/signrequests/show";
    }
    
    @RequestMapping(value = "/get-original-file/{id}", method = RequestMethod.GET)
    public void getOriginalFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
    	SignRequest document = SignRequest.findSignRequest(id);
        Document file = document.getOriginalFile();
        try {
            response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
            response.setContentType(file.getContentType());
            IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
        } catch (Exception e) {
            log.error("get file error", e);
        }
    }
    
    @RequestMapping(value = "/get-signed-file/{id}", method = RequestMethod.GET)
    public void getSignedFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
    	SignRequest document = SignRequest.findSignRequest(id);
        Document file = document.getSignedFile();
        try {
            response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
            response.setContentType(file.getContentType());
            IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
        } catch (Exception e) {
            log.error("get file error", e);
        }
    }    

}
