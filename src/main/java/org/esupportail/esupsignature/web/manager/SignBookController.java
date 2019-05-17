package org.esupportail.esupsignature.web.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.esupportail.esupsignature.domain.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.domain.SignRequestParams.SignType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.fs.EsupStockException;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
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
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

@RequestMapping("/manager/signbooks")
@Controller
@RooWebScaffold(path = "manager/signbooks", formBackingObject = SignBook.class)
@Transactional
public class SignBookController {

	private static final Logger logger = LoggerFactory.getLogger(SignBookController.class);
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "manager/signbooks";
	}

	@Resource
	private UserService userService;
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}

	@Resource
	private SignBookService signBookService;

	@Resource
	private SignRequestService signRequestService;
	
	@Resource
	private DocumentService documentService;

	@Resource
	private PdfService pdfService;

	void populateEditForm(Model uiModel, SignBook signBook) {
		uiModel.addAttribute("signBook", signBook);
		uiModel.addAttribute("users", User.findAllUsers());
		uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("targetTypes", Arrays.asList(DocumentIOType.values()));
		List<SignBookType> signBookTypes = new LinkedList<SignBookType>(Arrays.asList(SignBookType.values()));
		signBookTypes.remove(SignBookType.system);
		signBookTypes.remove(SignBookType.user);
		uiModel.addAttribute("signBookTypes", signBookTypes);
		uiModel.addAttribute("signTypes", Arrays.asList(SignRequestParams.SignType.values()));
		uiModel.addAttribute("newPageTypes", Arrays.asList(SignRequestParams.NewPageType.values()));
		addDateTimeFormatPatterns(uiModel);
		uiModel.addAttribute("signrequests", SignRequest.findAllSignRequests());
	}

	@RequestMapping(produces = "text/html")
	public String list(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "sortFieldName", required = false) String sortFieldName,
			@RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
		User user = userService.getUserFromAuthentication();
    	if(!userService.isUserReady(user)) {
			return "redirect:/user/users/?form";
		}
    	if(sortFieldName == null) {
    		sortFieldName = "signBookType";
    	}
		if (page != null || size != null) {
			int sizeNo = size == null ? 10 : size.intValue();
			//final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
			uiModel.addAttribute("signBooks", SignBook.findAllSignBooks(sortFieldName, sortOrder));
			float nrOfPages = (float) SignBook.countSignBooks() / sizeNo;
			uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
		} else {
			uiModel.addAttribute("signBooks", SignBook.findAllSignBooks(sortFieldName, sortOrder));
		}
		addDateTimeFormatPatterns(uiModel);
		return "manager/signbooks/list";
	}
	
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	User user = userService.getUserFromAuthentication();
    	populateEditForm(uiModel, new SignBook());
		List<SignBook> signBooks = new ArrayList<SignBook>();
		signBookService.creatorSignBook(user);
		signBooks.addAll(SignBook.findSignBooksBySignBookTypeEquals(SignBookType.system).getResultList());
		signBooks.addAll(SignBook.findSignBooksBySignBookTypeEquals(SignBookType.group).getResultList());
		signBooks.addAll(SignBook.findSignBooksBySignBookTypeEquals(SignBookType.user).getResultList());
		uiModel.addAttribute("allSignBooks", signBooks);
        return "manager/signbooks/create";
    }
	
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String create(@Valid SignBook signBook, @RequestParam("multipartFile") MultipartFile multipartFile,
			BindingResult bindingResult, @RequestParam(name = "signBooksIds", required = false) long[] signBooksIds, @RequestParam("signType") String signType, @RequestParam("newPageType") String newPageType,  Model uiModel, RedirectAttributes redirectAttrs, HttpServletRequest httpServletRequest) {
		if (bindingResult.hasErrors()) {
			populateEditForm(uiModel, signBook);
			return "manager/signbooks/create";
		}
		User user = userService.getUserFromAuthentication();
		SignBook signBookToUpdate = null;
		signBookToUpdate = SignBook.findSignBook(signBook.getId());
		signBook.setName(signBook.getName().trim());
		
		SignRequestParams signRequestParams = new SignRequestParams();
		signRequestParams.setSignType(SignType.valueOf(signType));
		signRequestParams.setNewPageType(NewPageType.valueOf(newPageType));
		signRequestParams.setSignPageNumber(1);
		try {		
			if (signBookToUpdate != null) {
				if(signBookToUpdate.getSignBookType().equals(SignBookType.user)) {
					return "redirect:/manager/signbooks/" + signBook.getId();
				}
				signBookService.updateSignBook(signBook, signBookToUpdate, signRequestParams, multipartFile);
			} else {
				if(signBook.getSignBookType().equals(SignBookType.workflow)) {
					List<SignBook> signBooks = new ArrayList<>();
					for(long signBookId : signBooksIds) {
						SignBook signBook2 = SignBook.findSignBook(signBookId);
						if(!signBooks.contains(signBook2)) {
							signBooks.add(signBook2);
						}
					}
					signBook.setSignBooks(signBooks);
					signBookService.createWorkflowSignBook(signBook, user, signRequestParams, multipartFile);
				} else {
					signBookService.createGroupSignBook(signBook, user, signRequestParams, multipartFile);
				}
			}
		} catch (EsupSignatureException e) {
			logger.error("enable to create signBookGroup", e);
			redirectAttrs.addFlashAttribute("messageCustom", signBook.getName() + " " + e.getMessage());
			return "redirect:/manager/signbooks?form";

		}

		uiModel.asMap().clear();
		return "redirect:/manager/signbooks/" + encodeUrlPathSegment(signBook.getId().toString(), httpServletRequest);
	}

    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = SignBook.findSignBook(id);
		if(signBook.getSignBookType().equals(SignBookType.user)) {
			return "redirect:/manager/signbooks/" + id;
		} else {
			if (!signBookService.checkUserManageRights(user, signBook)) {
				redirectAttrs.addFlashAttribute("messageCustom", "access error");
				return "redirect:/manager/signbooks/" + id;
			}
		}
		populateEditForm(uiModel, signBook);
        return "manager/signbooks/update";
    }
	
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid SignBook signBook, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, signBook);
            return "manager/signbooks/update";
        }
        uiModel.asMap().clear();
        signBook.merge();
        return "redirect:/manager/signbooks/" + encodeUrlPathSegment(signBook.getId().toString(), httpServletRequest);
    }
	
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
		addDateTimeFormatPatterns(uiModel);
		SignBook signBook = SignBook.findSignBook(id);
		Document modelFile = signBook.getModelFile();
		if (modelFile != null && modelFile.getSize() > 0) {
			uiModel.addAttribute("documentId", modelFile.getId());
			if(modelFile.getContentType().equals("application/pdf")) {
				PdfParameters pdfParameters = pdfService.getPdfParameters(modelFile.getJavaIoFile());
				uiModel.addAttribute("imagePagesSize", pdfParameters.getTotalNumberOfPages());
				int[] signFieldCoord = pdfService.getSignFieldCoord(modelFile.getJavaIoFile());
				if(signFieldCoord != null && signFieldCoord.length > 0) {
					uiModel.addAttribute("containsSignatureFields", true);			
				} else {
					uiModel.addAttribute("containsSignatureFields", false);
				}
			}
		}
		
		uiModel.addAttribute("lastSignRequestParam", signBook.getSignRequestParams().get(signBook.getSignRequestParams().size() - 1));
		uiModel.addAttribute("numberOfDocuments", signBook.getSignRequests().size());
		uiModel.addAttribute("signRequests", signBook.getSignRequests());
		uiModel.addAttribute("signBook", signBook);
		uiModel.addAttribute("itemId", id);
		return "manager/signbooks/show";
	}

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel, RedirectAttributes redirectAttrs) {
    	User user = userService.getUserFromAuthentication();
    	SignBook signBook = SignBook.findSignBook(id);
    		populateEditForm(uiModel, signBook);
		if(signBook.getSignBookType().equals(SignBookType.user)) {
			logger.error("can not delete user signBook");
			redirectAttrs.addFlashAttribute("messageCustom", "delete error");
			return "redirect:/manager/signbooks/" + id;
		} else {
			if (!signBookService.checkUserManageRights(user, signBook)) {
				redirectAttrs.addFlashAttribute("messageCustom", "delete error");
				return "redirect:/manager/signbooks/" + id;
			}    
		}
        signBook.remove();
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/manager/signbooks";
    }
	
	@RequestMapping(value = "/updateParams/{id}", method = RequestMethod.POST)
	public String updateParams(@PathVariable("id") Long id, @RequestParam(value = "xPos", required = true) int xPos,
			@RequestParam(value = "yPos", required = true) int yPos,
			@RequestParam(value = "signPageNumber", required = true) int signPageNumber,
			RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = SignBook.findSignBook(id);

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		signBook.getSignRequestParams().get(signBook.getSignRequestParams().size() - 1).setSignPageNumber(signPageNumber);
		signBook.getSignRequestParams().get(signBook.getSignRequestParams().size() - 1).setXPos(xPos);
		signBook.getSignRequestParams().get(signBook.getSignRequestParams().size() - 1).setYPos(yPos);
		signBook.setUpdateBy(user.getEppn());
		signBook.setUpdateDate(new Date());

		return "redirect:/manager/signbooks/" + id;
	}

	@RequestMapping(value = "/deleteParams/{id}", method = RequestMethod.POST)
	public String deleteParams(@PathVariable("id") Long id,
			@RequestParam(value = "signBookId", required = true) Long signBookId,
			RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		User user = userService.getUserFromAuthentication();
		SignRequestParams signRequestParams = SignRequestParams.findSignRequestParams(id); 
		SignBook signBook = SignBook.findSignBook(signBookId);
		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		if(!signBook.getSignRequestParams().get(0).equals(signRequestParams)) {
			signBook.getSignRequestParams().remove(signRequestParams);
			signBook.setUpdateBy(user.getEppn());
			signBook.setUpdateDate(new Date());
			signBook.merge();
		}
		//signRequestParams.remove();
		
		return "redirect:/manager/signbooks/" + signBookId;
	}
	
	@RequestMapping(value = "/addParams/{id}", method = RequestMethod.POST)
	public String addParams(@PathVariable("id") Long id, 
			RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = SignBook.findSignBook(id);

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		signBook.getSignRequestParams().add(signRequestService.getEmptySignRequestParams());
		signBook.setUpdateBy(user.getEppn());
		signBook.setUpdateDate(new Date());

		return "redirect:/manager/signbooks/" + id;
	}
	
	@RequestMapping(value = "/get-files-from-source/{id}", produces = "text/html")
	public String getFileFromSource(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = SignBook.findSignBook(id);

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		try {
			signBookService.importFilesFromSource(signBook, user);
		} catch (EsupSignatureIOException e) {
			redirectAttrs.addFlashAttribute("messageWarning", e.getMessage());
		} catch (EsupStockException e) {
			redirectAttrs.addFlashAttribute("messageError", e.getMessage());
		}
		
		return "redirect:/manager/signbooks/" + id;

	}

	@RequestMapping(value = "/send-files-to-target/{id}", produces = "text/html")
	public String sendFileToTarget(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) throws IOException, EsupSignatureException {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = SignBook.findSignBook(id);
		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		signBookService.exportFilesToTarget(signBook, user);
		return "redirect:/manager/signbooks/" + id;

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
