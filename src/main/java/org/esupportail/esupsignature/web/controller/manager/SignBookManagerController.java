package org.esupportail.esupsignature.web.controller.manager;

import com.google.common.collect.ImmutableList;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
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
public class SignBookManagerController {

	private static final Logger logger = LoggerFactory.getLogger(SignBookManagerController.class);
	
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
	private WorkflowService workflowService;
	
	@Resource
	private SignRequestParamsRepository signRequestParamsRepository; 

	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}


	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
		SignBook signBook = signBookRepository.findById(id).get();
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
		List<SignBookType> signBookTypes = new LinkedList<>(Arrays.asList(SignBookType.values()));
		signBookTypes.remove(SignBookType.system);
		uiModel.addAttribute("signBookTypes", signBookTypes);
		uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
		uiModel.addAttribute("signrequests", signRequestRepository.findAll());
	}

	@RequestMapping(produces = "text/html")
	public String list(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "sortFieldName", required = false) String sortFieldName,
			@RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
		User user = userService.getUserFromAuthentication();
    	if(sortFieldName == null) {
    		sortFieldName = "signBookType";
    	}

    	List<SignBook> signBooks = ImmutableList.copyOf(signBookRepository.findAll());
    	for (SignBook signBook : signBooks) {
			workflowService.setWorkflowsLabels(signBook.getWorkflowSteps());
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
			             @RequestParam(name = "multipartFile", required = false) MultipartFile multipartFile,
						 Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		SignBook signBook;
		try {
			signBook = signBookService.createSignBook(name, SignBookType.group, user, false);
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
		if (!signBookService.checkUserManageRights(user, signBook)) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/signbooks/" + id;
		}
		populateEditForm(uiModel, signBook);
        return "manager/signbooks/update";
    }
	
    @PostMapping(value = "/update/{id}")
    public String update(@PathVariable("id") Long id, @Valid SignBook signBook, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
		User user = userService.getUserFromAuthentication();
		SignBook signBookToUpdate = signBookRepository.findById(signBook.getId()).get();
		if(signBookToUpdate.getCreateBy().equals(user.getEppn())) {
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
		if (!signBookService.checkUserManageRights(user, signBook)) {
			redirectAttrs.addFlashAttribute("messageCustom", "Non autorisé");
			return "redirect:/manager/signbooks/" + id;
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

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	public String removeStepRecipient(@PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "recipientEmail") String recipientEmail,
									  RedirectAttributes redirectAttrs, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		SignBook signBook = signBookRepository.findById(id).get();
		WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
		if(user.getEppn().equals(signBook.getCreateBy())) {
			User recipientUserToRemove = userRepository.findByEmail(recipientEmail).get(0);
			workflowStep.getRecipients().remove(recipientUserToRemove.getId());
			workflowStepRepository.save(workflowStep);
		} else {
			logger.warn(user.getEppn() + " try to move " + signBook.getId() + " without rights");
		}
		return "redirect:/manager/signbooks/" + id + "#" + workflowStep.getId();
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

}
