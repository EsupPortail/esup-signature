package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.DocumentIOType;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/user/signbooks")
@Controller
@Transactional
public class UserSignBookController {

	private static final Logger logger = LoggerFactory.getLogger(UserSignBookController.class);

	@ModelAttribute("userMenu")
	public String getActiveMenu() {
		return "active";
	}
	
	@Autowired
	private SignBookRepository signBookRepository;

	@Autowired
	private SignRequestRepository signRequestRepository;
	
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
        uiModel.addAttribute("signrequests", signRequestRepository.findAll());
    }
    
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
		User user = userService.getUserFromAuthentication();
    	addDateTimeFormatPatterns(uiModel);
        SignBook signBook = signBookRepository.findById(id).get();
        populateEditForm(uiModel, signBook);
        List<SignRequest> signRequests = signBook.getSignRequests().stream().filter(signRequest -> user.getEppn().equals(signRequest.getCreateBy())).collect(Collectors.toList());
        uiModel.addAttribute("signRequests", signRequests);
        uiModel.addAttribute("itemId", id);
        uiModel.addAttribute("numberOfDocuments", signBook.getSignRequests().size());
        return "user/signbooks/show";
    }

    @RequestMapping(value = "/get-model-file/{id}", method = RequestMethod.GET)
    public void getModelFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
    	SignBook signBook = signBookRepository.findById(id).get();
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

	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid SignBook signBook, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, signBook);
            return "user/signbooks/create";
        }
        uiModel.asMap().clear();
        signBookRepository.save(signBook);
        return "redirect:/user/signbooks/" + encodeUrlPathSegment(signBook.getId().toString(), httpServletRequest);
    }

	@RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
        populateEditForm(uiModel, new SignBook());
        return "user/signbooks/create";
    }

	@RequestMapping(produces = "text/html")
    public String list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, @RequestParam(value = "sortFieldName", required = false) String sortFieldName, @RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
            uiModel.addAttribute("signbooks", signBookRepository.findAll());
            float nrOfPages = (float) signBookRepository.count() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("signbooks", signBookRepository.findAll());
        }
        addDateTimeFormatPatterns(uiModel);
        return "user/signbooks/list";
    }

	@RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid SignBook signBook, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, signBook);
            return "user/signbooks/update";
        }
        uiModel.asMap().clear();
        signBookRepository.save(signBook);
        return "redirect:/user/signbooks/" + encodeUrlPathSegment(signBook.getId().toString(), httpServletRequest);
    }

	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
        populateEditForm(uiModel, signBookRepository.findById(id).get());
        return "user/signbooks/update";
    }

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
        SignBook signBook = signBookRepository.findById(id).get();
        signBookRepository.delete(signBook);
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/user/signbooks";
    }

	void addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("signBook_createdate_date_format", "dd/MM/yyyy HH:mm");
        uiModel.addAttribute("signBook_updatedate_date_format", "dd/MM/yyyy HH:mm");
    }
}
