package org.esupportail.esupsignature.web.controller.manager;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.DocumentIOType;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequestParams.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

@CrossOrigin(origins = "*")
@RequestMapping("/manager/signbooks")
@Controller
@Transactional
@Scope(value = "session")
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


	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
		SignBook signBook = signBookRepository.findById(id).get();
		signRequestService.setSignBooksLabels(signBook.getWorkflowSteps());
		Document modelFile = signBook.getModelFile();
		if (modelFile != null && modelFile.getSize() != null) {
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
		uiModel.addAttribute("signBook", signBook);
		uiModel.addAttribute("signTypes", SignType.values());
		uiModel.addAttribute("itemId", id);
		return "manager/signbooks/show";
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
		signBookService.initCreatorSignBook();
    	if(!userService.isUserReady(user)) {
			return "redirect:/user/users/?form";
		}
    	if(sortFieldName == null) {
    		sortFieldName = "signBookType";
    	}

    	List<SignBook> signBooks = ImmutableList.copyOf(signBookRepository.findAll());

		for (SignBook signBook : signBooks) {
			signRequestService.setSignBooksLabels(signBook.getWorkflowSteps());
		}
		uiModel.addAttribute("signBooks", signBooks);
		return "manager/signbooks/list";
	}
	
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(@RequestParam(required = false) String type, Model uiModel) {
    	populateEditForm(uiModel, new SignBook());
		List<SignBook> signBooks = new ArrayList<>();
		signBooks.addAll(signBookRepository.findByExternal(false));
		uiModel.addAttribute("allSignBooks", signBooks);
		uiModel.addAttribute("type", type);
		if(type.equals("workflow")) {
			return "manager/signbooks/create-workflow";
		} else {
			return "manager/signbooks/create";
		}

    }
	
	@PostMapping(produces = "text/html")
	public String create(@RequestParam(name = "name") String name,
						 @RequestParam(name = "signBookType") String signBookType,
			             @RequestParam(name = "multipartFile", required = false) MultipartFile multipartFile,
						 Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook newSignBook = new SignBook();
		newSignBook.setName(name);
		newSignBook.setSignBookType(SignBookType.valueOf(signBookType));
		SignBook signBook;
		try {
			signBook = signBookService.createSignBook(newSignBook, user, multipartFile, false);
		} catch (EsupSignatureException e) {
			redirectAttrs.addAttribute("messageError", "Ce parapheur existe déjà");
			return "redirect:/manager/signbooks/";
		}
		return "redirect:/manager/signbooks/" + signBook.getId();
	}

	@PostMapping(value = "/add-recipients/{id}", produces = "text/html")
	public String addRecipients(@PathVariable("id") Long id,
						 @RequestParam(name = "recipientEmails") List<String> recipientEmails,
						 Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();
		if(signBook.getCreateBy().equals(user.getEppn())) {
			signBookService.addRecipient(signBook, recipientEmails);
		}
		return "redirect:/manager/signbooks/" + signBook.getId();
	}

	@DeleteMapping(value = "/remove-recipient/{id}", produces = "text/html")
	public String removeRecipients(@PathVariable("id") Long id,
								@RequestParam(name = "recipientEmail") String recipientEmail,
								Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();
		if(signBook.getCreateBy().equals(user.getEppn())) {
			signBookService.removeRecipient(signBook, recipientEmail);
		}
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
	
    @PostMapping(value = "/update/{id}")
    public String update(@PathVariable("id") Long id, @Valid SignBook signBook, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
		User user = userService.getUserFromAuthentication();
		SignBook signBookToUpdate = signBookRepository.findById(signBook.getId()).get();
		if(signBookToUpdate.getCreateBy().equals(user.getEppn())) {
			signBookToUpdate.setSourceType(signBook.getSourceType());
			signBookToUpdate.setTargetType(signBook.getTargetType());
			signBookToUpdate.setDocumentsSourceUri(signBook.getDocumentsSourceUri());
			signBookToUpdate.setDocumentsTargetUri(signBook.getDocumentsTargetUri());
			signBookToUpdate.setDescription(signBook.getDescription());
			signBookToUpdate.setUpdateBy(user.getEppn());
			signBookToUpdate.setUpdateDate(new Date());
			signBookRepository.save(signBookToUpdate);
		}
        return "redirect:/manager/signbooks/" + signBook.getId();

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
		//TODO with workflowsteps
		
		return "redirect:/manager/signbooks/" + signBookId;
	}

	@PostMapping(value = "/add-step/{id}")
	public String addStep(@PathVariable("id") Long id,
						  @RequestParam("signBookNames") List<String> singBookNames,
						  @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
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
			if(signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(signBookName), SignBookType.user).size() > 0) {
				SignBook signBookToAdd = signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(signBookName), SignBookType.user).get(0);
				workflowStep.getSignBooks().put(signBookToAdd.getId(), false);
			} else {
				if(signBookRepository.findByName(signBookName).size() > 0 ) {
					SignBook signBookToAdd = signBookRepository.findByName(signBookName).get(0);
					workflowStep.getSignBooks().put(signBookToAdd.getId(), false);

				}
			}
		}
		if(allSignToComplete ==null) {
			workflowStep.setAllSignToComplete(false);
		} else {
			workflowStep.setAllSignToComplete(allSignToComplete);
		}
		workflowStep.setSignRequestParams(signRequestService.getEmptySignRequestParams());
		workflowStep.getSignRequestParams().setSignType(SignType.valueOf(signType));
		workflowStepRepository.save(workflowStep);
		signBook.getWorkflowSteps().add(workflowStep);
		return "redirect:/manager/signbooks/" + id;
	}

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	public String removeStepRecipient(@PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "recipientName") String recipientName,
									  RedirectAttributes redirectAttrs, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignBook signBook = signBookRepository.findById(id).get();
		WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
		if(user.getEppn().equals(signBook.getCreateBy())) {
			SignBook signBookToRemove = signBookRepository.findByName(recipientName).get(0);
			workflowStep.getSignBooks().remove(signBookToRemove.getId());
			workflowStepRepository.save(workflowStep);
		} else {
			logger.warn(user.getEppn() + " try to move " + signBook.getId() + " without rights");
		}
		return "redirect:/manager/signbooks/" + id + "#" + workflowStep.getId();
	}

	@RequestMapping(value = "/send-to-signbook/{id}/{workflowStepId}", method = RequestMethod.GET)
	public String sendToSignBook(@PathVariable("id") Long id,
								 @PathVariable("workflowStepId") Long workflowStepId,
								 @RequestParam(value = "signBookNames", required = true) String[] signBookNames,
								 RedirectAttributes redirectAttrs, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignBook signBook = signBookRepository.findById(id).get();
		if(user.getEppn().equals(signBook.getCreateBy())) {
			WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
			for(String signBookName : signBookNames) {
				if(signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(signBookName), SignBookType.user).size() > 0) {
					SignBook signBookToAdd = signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(signBookName), SignBookType.user).get(0);
					workflowStep.getSignBooks().put(signBookToAdd.getId(), false);
				} else {
					if(signBookRepository.findByName(signBookName).size() > 0 ) {
						SignBook signBookToAdd = signBookRepository.findByName(signBookName).get(0);
						workflowStep.getSignBooks().put(signBookToAdd.getId(), false);

					}
				}
			}
		} else {
			logger.warn(user.getEppn() + " try to move " + signBook.getId() + " without rights");
		}
		return "redirect:/manager/signbooks/" + id;
	}

	@RequestMapping(value = "/toggle-need-all-sign/{id}/{step}", method = RequestMethod.GET)
	public String toggleNeedAllSign(@PathVariable("id") Long id,@PathVariable("step") Integer step) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();
		if(user.getEppn().equals(signBook.getCreateBy())) {
			Long stepId = signRequestService.toggleNeedAllSign(signBook, step);
			return "redirect:/manager/signbooks/" + id + "#" + stepId;
		}
		return "redirect:/manager/signbooks/";
	}

	@RequestMapping(value = "/change-step-sign-type/{id}/{step}", method = RequestMethod.GET)
	public String changeStepSignType(@PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name="signType") SignType signType) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();
		if(user.getEppn().equals(signBook.getCreateBy())) {
			signRequestService.changeSignType(signBook, step, null, signType);
			return "redirect:/manager/signbooks/" + id;
		}
		return "redirect:/manager/signbooks/";
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
			RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		signBook.setUpdateBy(user.getEppn());
		signBook.setUpdateDate(new Date());
		return "redirect:/manager/signbooks/" + id;
	}

	@RequestMapping(value = "/get-files-from-source/{id}", produces = "text/html")
	public String getFileFromSource(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook = signBookRepository.findById(id).get();

		if (!signBook.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		List<FsFile> fsFiles = signBookService.importFilesFromSource(signBook, user);
		if(fsFiles.size() == 0) {
			redirectAttrs.addFlashAttribute("messageError", "Aucun fichier à importer");
		} else {
			redirectAttrs.addFlashAttribute("messageInfo", fsFiles.size() + " ficher(s) importé(s)");
		}
		return "redirect:/manager/signbooks/" + id;
	}

}
