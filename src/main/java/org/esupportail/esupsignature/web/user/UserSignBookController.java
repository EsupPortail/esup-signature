package org.esupportail.esupsignature.web.user;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

@RequestMapping("/user/signbooks")
@Controller
@RooWebScaffold(path = "user/signbooks", formBackingObject = SignBook.class)
@Transactional
public class UserSignBookController {

	private static final Logger logger = LoggerFactory.getLogger(UserSignBookController.class);

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/signbooks";
	}
	
	@Resource
	private UserService userService;
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}
	
	@Resource
	private SignRequestService signRequestService;
	
	@Resource
	private DocumentService documentService;
	
	@Resource
	private PdfService pdfService;
	
    void populateEditForm(Model uiModel, SignBook signBook) {
        uiModel.addAttribute("signBook", signBook);
        uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
        //uiModel.addAttribute("signBookTypes", Arrays.asList(SignBookType.values()));
        uiModel.addAttribute("signTypes", Arrays.asList(SignRequestParams.SignType.values()));
        uiModel.addAttribute("newPageTypes", Arrays.asList(SignRequestParams.NewPageType.values()));        
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("signrequests", SignRequest.findAllSignRequests());
    }
    
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
		User user = userService.getUserFromAuthentication();
    	addDateTimeFormatPatterns(uiModel);
        SignBook signBook = SignBook.findSignBook(id);
        populateEditForm(uiModel, signBook);
        List<SignRequest> signRequests = signBook.getSignRequests().stream().filter(signRequest -> user.getEppn().equals(signRequest.getCreateBy())).collect(Collectors.toList());
        uiModel.addAttribute("signRequests", signRequests);
        uiModel.addAttribute("itemId", id);
        uiModel.addAttribute("numberOfDocuments", signBook.getSignRequests().size());
        return "user/signbooks/show";
    }

    @RequestMapping(value = "/addDoc/{id}", method = RequestMethod.POST)
    public String addDoc(@PathVariable("id") Long id,
    		@RequestParam("multipartFile") MultipartFile multipartFile, RedirectAttributes redirectAttrs, HttpServletResponse response, Model model, HttpServletRequest request) throws IOException {
		Document documentToAdd = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
    	if(documentToAdd != null) {
	    	User user = userService.getUserFromAuthentication();
	    	user.setIp(request.getRemoteAddr());
			SignBook signBook = SignBook.findSignBook(id);
			long[] signBookIds = {signBook.getId()};
			//signRequestService.createSignRequest(new SignRequest(), user, documentToAdd, signBook.getSignRequestParams(), signBookIds);
		} else {
			redirectAttrs.addFlashAttribute("messageCustom", "file is required");
		}
		
	    return "redirect:/user/signbooks/" + id;
    }
    
    @RequestMapping(value = "/get-model-file/{id}", method = RequestMethod.GET)
    public void getModelFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
    	SignBook signBook = SignBook.findSignBook(id);
        Document file = signBook.getModelFile();
        try {
            response.setHeader("Content-Disposition", "inline;filename=\"" + file.getFileName() + "\"");
            response.setContentType(file.getContentType());
            IOUtils.copy(file.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }
    
    String encodeUrlPathSegment(String pathSegment, HttpServletRequest httpServletRequest) {
        String enc = httpServletRequest.getCharacterEncoding();
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        pathSegment = UriUtils.encodePathSegment(pathSegment, enc);
        return pathSegment;
    }
}
