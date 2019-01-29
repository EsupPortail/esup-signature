package org.esupportail.esupsignature.web.manager;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.NewPageType;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignBook.SignType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.PdfService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/manager/signbooks")
@Controller
@RooWebScaffold(path = "manager/signbooks", formBackingObject = SignBook.class)
@Transactional
public class SignBookController {

	private static final Logger log = LoggerFactory.getLogger(SignBookController.class);

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "manager/signbooks";
	}
	
	@Resource
	UserService userService;
	
	@Resource
	DocumentService documentService;
	
	@Resource
	PdfService pdfService;
	
    void populateEditForm(Model uiModel, SignBook signBook) {
        uiModel.addAttribute("signBook", signBook);
        uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
        uiModel.addAttribute("signBookTypes", Arrays.asList(SignBookType.values()));
        uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
        uiModel.addAttribute("newPageTypes", Arrays.asList(NewPageType.values()));        
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("signrequests", SignRequest.findAllSignRequests());
    }
    
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid SignBook signBook, @RequestParam("multipartFile") MultipartFile multipartFile, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, signBook);
            return "manager/signbooks/create";
        }
    
        String eppn = userService.getEppnFromAuthentication();
        try {
        	signBook.setCreateBy(eppn);
        	signBook.setCreateDate(new Date());
        	if(multipartFile != null) {
        		signBook.setModelFile(documentService.addFile(multipartFile, multipartFile.getOriginalFilename()));
        	}
			Map<String, String> params = new HashMap<>();
			params.put("signType", signBook.getSignType());
			params.put("newPageType", signBook.getNewPageType());
			params.put("signPageNumber", "1");
			params.put("xPos", "0");
			params.put("yPos", "0");
			signBook.setParams(params);
			signBook.persist();
        } catch (IOException e) {
        	log.error("Create file error", e);
		}
        uiModel.asMap().clear();
        signBook.persist();
        return "redirect:/manager/signbooks/" + encodeUrlPathSegment(signBook.getId().toString(), httpServletRequest);
    }
    
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
        addDateTimeFormatPatterns(uiModel);
        SignBook signBook = SignBook.findSignBook(id);
        Document modelFile = signBook.getModelFile();
        uiModel.addAttribute("imagePagesSize", pdfService.getTotalNumberOfPages(modelFile.getBigFile().toJavaIoFile()));
        uiModel.addAttribute("documentId", modelFile.getId());
        uiModel.addAttribute("signbook", signBook);
        uiModel.addAttribute("itemId", id);
        return "manager/signbooks/show";
    }
    
    @RequestMapping(value = "/updateParams/{id}", method = RequestMethod.POST)
    public String updateParams(@PathVariable("id") Long id, 
    		@RequestParam(value = "xPos", required=true) int xPos,
    		@RequestParam(value = "yPos", required=true) int yPos,
    		@RequestParam(value = "signPageNumber", required=true) int signPageNumber, RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		SignBook signBook = SignBook.findSignBook(id);

		if(!signBook.getCreateBy().equals(user.getEppn())) {
        	redirectAttrs.addFlashAttribute("messageCustom", "access error");
    		return "redirect:/manager/signbooks/" + id;
    	}

		Map<String, String> params = signBook.getParams();
    	params.put("signPageNumber", String.valueOf(signPageNumber));
		params.put("xPos", String.valueOf(xPos));
		params.put("yPos", String.valueOf(yPos));
		signBook.setParams(params);
	    signBook.setUpdateBy(eppn);
	    signBook.setUpdateDate(new Date());

	    return "redirect:/manager/signbooks/" + id;
    }
    
    @RequestMapping(produces = "text/html")
    public String list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
    	String eppn = userService.getEppnFromAuthentication();
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
            uiModel.addAttribute("signbooks", SignBook.findSignBooksByCreateByEquals(eppn, sortFieldName, sortOrder).getResultList());
            float nrOfPages = (float) SignBook.countSignBooks() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("signbooks", SignBook.findSignBooksByCreateByEquals(eppn, sortFieldName, sortOrder).getResultList());
        }
        addDateTimeFormatPatterns(uiModel);
        return "manager/signbooks/list";
    }
}
