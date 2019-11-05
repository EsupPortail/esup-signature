package org.esupportail.esupsignature.web.controller.manager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.DocumentIOType;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.entity.SignRequestParams.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.fs.EsupStockException;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

@RequestMapping("/manager/signbooks")
@Controller
@Transactional
public class SignBookController {

	private static final Logger logger = LoggerFactory.getLogger(SignBookController.class);
	
	@ModelAttribute("managerMenu")
	public String getActiveMenu() {
		return "active";
	}

	@Resource
	private UserRepository userRepository;
	
	@Resource
	private UserService userService;
	
	@Resource
	private SignBookRepository signBookRepository;

	@Resource
	private WorkflowStepRepository workflowStepRepository;

	@Resource
	private SignBookService signBookService;

	@Resource
	private SignRequestRepository signRequestRepository;
	
	@Resource
	private SignRequestService signRequestService;
	
	@Resource
	private SignRequestParamsRepository signRequestParamsRepository; 

	@Resource
	private PdfService pdfService;

	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}



	void populateEditForm(Model uiModel, SignBook signBook) {
		uiModel.addAttribute("signBook", signBook);
		uiModel.addAttribute("users", userRepository.findAll());
		uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("targetTypes", Arrays.asList(DocumentIOType.values()));
		List<SignBookType> signBookTypes = new LinkedList<SignBookType>(Arrays.asList(SignBookType.values()));
		signBookTypes.remove(SignBookType.system);
		signBookTypes.remove(SignBookType.user);
		uiModel.addAttribute("signBookTypes", signBookTypes);
		uiModel.addAttribute("signTypes", Arrays.asList(SignRequestParams.SignType.values()));
		uiModel.addAttribute("newPageTypes", Arrays.asList(SignRequestParams.NewPageType.values()));
		uiModel.addAttribute("signrequests", signRequestRepository.findAll());
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

    	List<SignBook> signBooks = signBookRepository.findByNotCreateBy("System");

		for (SignBook signBook : signBooks) {
			signRequestService.setSignBooksLabels(signBook.getWorkflowSteps());
		}
		uiModel.addAttribute("signBooks", signBooks);
		return "manager/signbooks/list";
	}
	
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(@RequestParam(required = false) String type, Model uiModel) {
    	populateEditForm(uiModel, new SignBook());
		List<SignBook> signBooks = new ArrayList<SignBook>();
		signBookService.creatorSignBook();
		signBooks.addAll(signBookRepository.findByExternal(false));
		uiModel.addAttribute("allSignBooks", signBooks);
		uiModel.addAttribute("type", type);
        return "manager/signbooks/create";
    }
	
	@PostMapping(produces = "text/html")
	public String create(SignBook signBook, @RequestParam("type") String type, @RequestParam("multipartFile") MultipartFile multipartFile,
			@RequestParam(name = "signBooksIds", required = false) Long[] signBooksIds, @RequestParam("signType") String signType, @RequestParam("newPageType") String newPageType,  Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBookToUpdate = null;
		if(signBook.getId() != null) {
			signBookToUpdate = signBookRepository.findById(signBook.getId()).get();
		}
		signBook.setName(signBook.getName().trim());
		if(signBook.getSourceType() != null && signBook.getDocumentsSourceUri().isEmpty()) {
			String defaultSourceUri =  signBook.getSignBookType().toString() + "/" + signBook.getName();
			signBook.setDocumentsSourceUri(defaultSourceUri);
		}
		if(signBook.getTargetType() != null && signBook.getDocumentsTargetUri().isEmpty()) {
			String defaultTargetUri =  signBook.getSignBookType().toString() + "/" + signBook.getName() + "/signed";
			signBook.setDocumentsTargetUri(defaultTargetUri);
		}
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
				if("workflow".equals(type)) {
					signBook.setSignBookType(SignBookType.workflow);
				} else {
					signBook.setSignBookType(SignBookType.group);
				}
				signBookService.createSignBook(signBook, user, signRequestParams, multipartFile, false);

			}
		} catch (EsupSignatureException e) {
			logger.error("enable to create signBookGroup", e);
			redirectAttrs.addFlashAttribute("messageCustom", signBook.getName() + " " + e.getMessage());
			return "redirect:/manager/signbooks?form";

		}

		uiModel.asMap().clear();
		return "redirect:/manager/signbooks/" + signBook.getId();
	}

    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();
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
        signBookRepository.save(signBook);
        return "redirect:/manager/signbooks/" + encodeUrlPathSegment(signBook.getId().toString(), httpServletRequest);
    }
	
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
		SignBook signBook = signBookRepository.findById(id).get();
		signRequestService.setSignBooksLabels(signBook.getWorkflowSteps());
		Document modelFile = signBook.getModelFile();
		if (modelFile != null && modelFile.getSize() > 0) {
			uiModel.addAttribute("documentId", modelFile.getId());
			if(modelFile.getContentType().equals("application/pdf")) {
				PDDocument pdDocument = PDDocument.load(modelFile.getInputStream());
				PdfParameters pdfParameters = pdfService.getPdfParameters(pdDocument);
				uiModel.addAttribute("imagePagesSize", pdfParameters.getTotalNumberOfPages());
				int[] signFieldCoord = pdfService.getSignFieldCoord(pdDocument, 0);
				if(signFieldCoord != null && signFieldCoord.length > 0) {
					uiModel.addAttribute("containsSignatureFields", true);			
				} else {
					uiModel.addAttribute("containsSignatureFields", false);
				}
			}
		}
//		uiModel.addAttribute("numberOfDocuments", signBook.getSignRequests().size());
//		uiModel.addAttribute("signRequests", signBook.getSignRequests());
		uiModel.addAttribute("signBook", signBook);
		uiModel.addAttribute("signTypes", SignType.values());
		uiModel.addAttribute("itemId", id);
		return "manager/signbooks/show";
	}

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel, RedirectAttributes redirectAttrs) {
    	User user = userService.getUserFromAuthentication();
    	SignBook signBook = signBookRepository.findById(id).get();
    		populateEditForm(uiModel, signBook);
		if(signBook.getSignBookType().equals(SignBookType.user)) {
			logger.error("can not delete user signBook");
			redirectAttrs.addFlashAttribute("messageCustom", "Impossible de supprimer un parapheur utilisateur");
			return "redirect:/manager/signbooks/" + id;
		} else {
			if (!signBookService.checkUserManageRights(user, signBook)) {
				redirectAttrs.addFlashAttribute("messageCustom", "Non autorisé");
				return "redirect:/manager/signbooks/" + id;
			}    
		}

//		if(signBook.getSignRequests().size() > 0) {
//			redirectAttrs.addFlashAttribute("messageCustom", "Le parapheur n'est pas vide");
//			return "redirect:/manager/signbooks/" + id;
//		}

		signBookService.deleteSignBook(signBook);
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/manager/signbooks";
    }
	
	@RequestMapping(value = "/update-params/{id}", method = RequestMethod.POST)
	public String updateParams(@PathVariable("id") Long id, @RequestParam(value = "xPos", required = true) int xPos,
			@RequestParam(value = "yPos", required = true) int yPos,
			@RequestParam(value = "signPageNumber", required = true) int signPageNumber,
			RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		signBook.getSignRequestParams().setSignPageNumber(signPageNumber);
		signBook.getSignRequestParams().setXPos(xPos);
		signBook.getSignRequestParams().setYPos(yPos);
		signBook.setUpdateBy(user.getEppn());
		signBook.setUpdateDate(new Date());

		return "redirect:/manager/signbooks/" + id;
	}

	@RequestMapping(value = "/delete-params/{id}", method = RequestMethod.POST)
	public String deleteParams(@PathVariable("id") Long id,
			@RequestParam(value = "signBookId", required = true) Long signBookId,
			RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		User user = userService.getUserFromAuthentication();
		SignRequestParams signRequestParams = signRequestParamsRepository.findById(id).get(); 
		SignBook signBook = signBookRepository.findById(signBookId).get();
		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		if(!signBook.getSignRequestParams().equals(signRequestParams)) {
			signBook.setSignRequestParams(null);
			signBook.setUpdateBy(user.getEppn());
			signBook.setUpdateDate(new Date());
			signBookRepository.save(signBook);
		}
		//signRequestParams.remove();
		
		return "redirect:/manager/signbooks/" + signBookId;
	}

	@PostMapping(value = "/add-step/{id}")
	public String addStep(@PathVariable("id") Long id,
						  @RequestParam("signBookNames") List<String> singBookNames,
						  @RequestParam(name="allSignToComplete", required = false) String allSignToComplete,
						  @RequestParam("signType") String signType,
						  RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();
		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		WorkflowStep workflowStep = new WorkflowStep();
		for(String signBookName : singBookNames) {
			SignBook signBook1 = signBookRepository.findByRecipientEmails(Arrays.asList(signBookName)).get(0);
			workflowStep.getSignBooks().put(signBook1.getId(), false);
		}
		workflowStep.setAllSignToComplete(Boolean.valueOf(allSignToComplete));
		workflowStep.setSignRequestParams(signRequestService.getEmptySignRequestParams());
		workflowStep.getSignRequestParams().setSignType(SignType.valueOf(signType));
		workflowStepRepository.save(workflowStep);
		signBook.getWorkflowSteps().add(workflowStep);
		return "redirect:/manager/signbooks/" + id;
	}

	@PutMapping(value = "/update-step/{id}/{stepNumber}")
	public String updateStep(@PathVariable("id") Long id,
						 @PathVariable("stepNumber") Integer stepNumber,
						  @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
						  @RequestParam("signType") String signType,
						  RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();
		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		WorkflowStep workflowStep = signBook.getWorkflowSteps().get(stepNumber);
		workflowStep.getSignRequestParams().setSignType(SignType.valueOf(signType));
		if(allSignToComplete ==null) {
			workflowStep.setAllSignToComplete(false);
		} else {
			workflowStep.setAllSignToComplete(allSignToComplete);
		}
		workflowStepRepository.save(workflowStep);
		return "redirect:/manager/signbooks/" + id;
	}

	@DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
	public String addStep(@PathVariable("id") Long id, @PathVariable("stepNumber") Integer stepNumber, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();
		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		WorkflowStep workflowStep = signBook.getWorkflowSteps().get(stepNumber);
		signBook.getWorkflowSteps().remove(workflowStep);
		signBookRepository.save(signBook);
		workflowStepRepository.delete(workflowStep);
		return "redirect:/manager/signbooks/" + id;
	}

	@RequestMapping(value = "/add-params/{id}", method = RequestMethod.POST)
	public String addParams(@PathVariable("id") Long id, 
			RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		signBook.setSignRequestParams(signRequestService.getEmptySignRequestParams());
		signBook.setUpdateBy(user.getEppn());
		signBook.setUpdateDate(new Date());

		return "redirect:/manager/signbooks/" + id;
	}
	
	@RequestMapping(value = "/get-files-from-source/{id}", produces = "text/html")
	public String getFileFromSource(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		try {
			signBookService.importFilesFromSource(signBook, user);
		} catch (EsupSignatureIOException e) {
			redirectAttrs.addFlashAttribute("messageCustom", e.getMessage());
		} catch (EsupStockException e) {
			redirectAttrs.addFlashAttribute("messageCustom", e.getMessage());
		}
		
		return "redirect:/manager/signbooks/" + id;

	}

	@RequestMapping(value = "/send-files-to-target/{id}", produces = "text/html")
	public String sendFileToTarget(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) throws IOException, EsupSignatureException {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();
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
