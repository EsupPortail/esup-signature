package org.esupportail.esupsignature.web.user;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.NewPageType;
import org.esupportail.esupsignature.domain.SignBook.SignType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.PdfService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/user/signbooks")
@Controller
@RooWebScaffold(path = "user/signbooks", formBackingObject = SignBook.class)
public class UserSignBookController {

	private static final Logger log = LoggerFactory.getLogger(UserSignBookController.class);

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/signbooks";
	}
	
	@Resource
	private SignRequestService signRequestService;
	
	@Resource
	UserService userService;
	
	@Resource
	DocumentService documentService;
	
	@Resource
	PdfService pdfService;
	
    void populateEditForm(Model uiModel, SignBook signBook) {
        uiModel.addAttribute("signBook", signBook);
        uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
        //uiModel.addAttribute("signBookTypes", Arrays.asList(SignBookType.values()));
        uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
        uiModel.addAttribute("newPageTypes", Arrays.asList(NewPageType.values()));        
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("signrequests", SignRequest.findAllSignRequests());
    }
    
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
		String eppn = userService.getEppnFromAuthentication();
    	addDateTimeFormatPatterns(uiModel);
        SignBook signBook = SignBook.findSignBook(id);
        uiModel.addAttribute("signbook", signBook);
        List<SignRequest> signRequests = signBook.getSignRequests().stream().filter(signRequest -> eppn.equals(signRequest.getCreateBy())).collect(Collectors.toList());
        uiModel.addAttribute("signRequests", signRequests);
        uiModel.addAttribute("itemId", id);
        uiModel.addAttribute("numberOfDocuments", signBook.getSignRequests().size());
        return "user/signbooks/show";
    }

    @RequestMapping(value = "/addDoc/{id}", method = RequestMethod.POST)
    public String addDoc(@PathVariable("id") Long id,
    		@RequestParam("multipartFile") MultipartFile multipartFile, RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) throws IOException {
		Document documentToAdd = documentService.addFile(multipartFile, multipartFile.getOriginalFilename());
    	if(documentToAdd != null) {
	    	String eppn = userService.getEppnFromAuthentication();
			SignBook signBook = SignBook.findSignBook(id);

			SignRequest signRequest = signRequestService.createSignRequest(eppn, documentToAdd, new HashMap<String, String>(signBook.getParams()), signBook.getRecipientEmail());
	        signBook.getSignRequests().add(signRequest);
	        signBook.persist();
		} else {
			redirectAttrs.addFlashAttribute("messageCustom", "file is required");
		}
		
	    return "redirect:/user/signbooks/" + id;
    }
    
}
