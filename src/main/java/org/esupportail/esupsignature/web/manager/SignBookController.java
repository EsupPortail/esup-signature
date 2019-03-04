package org.esupportail.esupsignature.web.manager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.esupportail.esupsignature.domain.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.domain.SignRequestParams.SignType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
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

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "manager/signbooks";
	}

	@Resource
	SignBookService signBookService;
	
	@Resource
	UserService userService;

	@Resource
	DocumentService documentService;

	@Resource
	PdfService pdfService;

	void populateEditForm(Model uiModel, SignBook signBook) {
		uiModel.addAttribute("signBook", signBook);
		uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("targetTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("signTypes", Arrays.asList(SignRequestParams.SignType.values()));
		uiModel.addAttribute("newPageTypes", Arrays.asList(SignRequestParams.NewPageType.values()));
		addDateTimeFormatPatterns(uiModel);
		uiModel.addAttribute("signrequests", SignRequest.findAllSignRequests());
	}
	
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String create(@Valid SignBook signBook, @RequestParam("multipartFile") MultipartFile multipartFile,
			BindingResult bindingResult, @RequestParam("signType") String signType, @RequestParam("newPageType") String newPageType,  Model uiModel, HttpServletRequest httpServletRequest) throws IOException {
		if (bindingResult.hasErrors()) {
			populateEditForm(uiModel, signBook);
			return "manager/signbooks/create";
		}
		String eppn = userService.getEppnFromAuthentication();
		SignBook signBookToUpdate = null;
		signBookToUpdate = SignBook.findSignBook(signBook.getId());
		if (signBookToUpdate != null) {
			signBookToUpdate.setName(signBook.getName());
			signBookToUpdate.setRecipientEmail(signBook.getRecipientEmail());
			signBookToUpdate.setDocumentsSourceUri(signBook.getDocumentsSourceUri());
			signBookToUpdate.setSourceType(signBook.getSourceType());
			signBookToUpdate.setDocumentsTargetUri(signBook.getDocumentsTargetUri());
			signBookToUpdate.setTargetType(signBook.getTargetType());
			signBookToUpdate.getSignRequestParams().setSignType(SignType.valueOf(signType));
			signBookToUpdate.getSignRequestParams().setNewPageType(NewPageType.valueOf(newPageType));
			Document newModel = documentService.addFile(multipartFile, multipartFile.getOriginalFilename());
			if(newModel != null) {
				Document oldModel = signBookToUpdate.getModelFile();
				signBookToUpdate.setModelFile(newModel);
				oldModel.remove();
			}
			signBookToUpdate.merge();
		} else {
			signBook.setCreateBy(eppn);
			signBook.setCreateDate(new Date());
			signBook.setModelFile(documentService.addFile(multipartFile, multipartFile.getOriginalFilename()));
			SignRequestParams signRequestParams = new SignRequestParams();
			signRequestParams.setSignType(SignType.valueOf(signType));
			signRequestParams.setNewPageType(NewPageType.valueOf(newPageType));
			signRequestParams.setSignPageNumber(1);
			signRequestParams.setXPos(0);
			signRequestParams.setYPos(0);
			signRequestParams.persist();
			signBook.setSignRequestParams(signRequestParams);
			signBook.persist();
		}
		uiModel.asMap().clear();
		return "redirect:/manager/signbooks/" + encodeUrlPathSegment(signBook.getId().toString(), httpServletRequest);
	}

	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
		addDateTimeFormatPatterns(uiModel);
		SignBook signBook = SignBook.findSignBook(id);
		Document modelFile = signBook.getModelFile();
		if (modelFile.getSize() > 0) {
			uiModel.addAttribute("documentId", modelFile.getId());
			if(modelFile.getContentType().equals("application/pdf")) {
				PdfParameters pdfParameters = pdfService.getPdfParameters(modelFile.getJavaIoFile());
				uiModel.addAttribute("imagePagesSize", pdfParameters.getTotalNumberOfPages());
			}
		}

		uiModel.addAttribute("numberOfDocuments", signBook.getSignRequests().size());
		uiModel.addAttribute("signRequests", signBook.getSignRequests());
		uiModel.addAttribute("signbook", signBook);
		uiModel.addAttribute("itemId", id);
		return "manager/signbooks/show";
	}

	@RequestMapping(value = "/updateParams/{id}", method = RequestMethod.POST)
	public String updateParams(@PathVariable("id") Long id, @RequestParam(value = "xPos", required = true) int xPos,
			@RequestParam(value = "yPos", required = true) int yPos,
			@RequestParam(value = "signPageNumber", required = true) int signPageNumber,
			RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		SignBook signBook = SignBook.findSignBook(id);

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		signBook.getSignRequestParams().setSignPageNumber(signPageNumber);
		signBook.getSignRequestParams().setXPos(xPos);
		signBook.getSignRequestParams().setYPos(yPos);
		signBook.setUpdateBy(eppn);
		signBook.setUpdateDate(new Date());

		return "redirect:/manager/signbooks/" + id;
	}

	@RequestMapping(produces = "text/html")
	public String list(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "sortFieldName", required = false) String sortFieldName,
			@RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
		String eppn = userService.getEppnFromAuthentication();
		if (page != null || size != null) {
			int sizeNo = size == null ? 10 : size.intValue();
			//final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
			uiModel.addAttribute("signbooks",
					SignBook.findSignBooksByCreateByEquals(eppn, sortFieldName, sortOrder).getResultList());
			float nrOfPages = (float) SignBook.countSignBooks() / sizeNo;
			uiModel.addAttribute("maxPages",
					(int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
		} else {
			uiModel.addAttribute("signbooks",
					SignBook.findSignBooksByCreateByEquals(eppn, sortFieldName, sortOrder).getResultList());
		}
		addDateTimeFormatPatterns(uiModel);
		return "manager/signbooks/list";
	}

	@RequestMapping(value = "/get-files-from-source/{id}", produces = "text/html")
	public String getFileFromSource(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) throws IOException {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		SignBook signBook = SignBook.findSignBook(id);

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		
		signBookService.importFilesFromSource(signBook, user);
		return "redirect:/manager/signbooks/" + id;

	}

	@RequestMapping(value = "/send-files-to-target/{id}", produces = "text/html")
	public String sendFileToTarget(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) throws IOException, EsupSignatureException {
		String eppn = userService.getEppnFromAuthentication();
		User user = User.findUsersByEppnEquals(eppn).getSingleResult();
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
