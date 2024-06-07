package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.dto.js.JsSlimSelect;
import org.esupportail.esupsignature.dto.UserDto;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequestMapping("/admin/signbooks")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class SignBookAdminController {

	private static final Logger logger = LoggerFactory.getLogger(SignBookAdminController.class);

	@ModelAttribute("adminMenu")
	public String getAdminMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "adminsignrequests";
	}

	@Resource
	private FormService formService;

	@Resource
	private UserService userService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private SignBookRepository signBookRepository;

	@Resource
	private TemplateEngine templateEngine;

	@GetMapping
	public String list(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
					   @RequestParam(value = "statusFilter", required = false) String statusFilter,
					   @RequestParam(value = "workflowFilter", required = false) String workflowFilter,
					   @RequestParam(value = "docTitleFilter", required = false) String docTitleFilter,
					   @RequestParam(value = "creatorFilter", required = false) String creatorFilter,
					   @RequestParam(value = "dateFilter", required = false) String dateFilter,
					   @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
		if(statusFilter != null && (statusFilter.isEmpty() || statusFilter.equals("all"))) {
			statusFilter = null;
		}
		if(workflowFilter != null && (workflowFilter.isEmpty() || workflowFilter.equals("all"))) {
			workflowFilter = null;
		}
		if(creatorFilter != null && (creatorFilter.isEmpty() || creatorFilter.equals("all"))) {
			creatorFilter = null;
		}
		if(docTitleFilter != null && (docTitleFilter.isEmpty() || docTitleFilter.equals("all"))) {
			docTitleFilter = null;
		}
		Page<SignBook> signBooks = signBookService.getAllSignBooks(statusFilter, workflowFilter, docTitleFilter, creatorFilter, dateFilter, pageable);
		model.addAttribute("statusFilter", statusFilter);
		model.addAttribute("signBooks", signBooks);
		model.addAttribute("creators", userService.getAllUsersDto());
		model.addAttribute("nbEmpty", signBookService.countEmpty(userEppn));
		model.addAttribute("statuses", SignRequestStatus.values());
		model.addAttribute("forms", formService.getAllForms());
		model.addAttribute("workflows", workflowService.getAllWorkflows());
		model.addAttribute("workflowFilter", workflowFilter);
		model.addAttribute("creatorFilter", creatorFilter);
		if(docTitleFilter != "%") model.addAttribute("docTitleFilter", docTitleFilter);
		model.addAttribute("dateFilter", dateFilter);
		model.addAttribute("workflowNames", signBookRepository.findAllWorkflowNames());
		return "admin/signbooks/list";
	}

	@GetMapping("/creators")
	@ResponseBody
	public Object[] creators() {
		List<JsSlimSelect> slimSelectDtos = new ArrayList<>();
		slimSelectDtos.add(new JsSlimSelect("Tout", ""));
		for(UserDto userDto : userService.getAllUsersDto()) {
			slimSelectDtos.add(new JsSlimSelect(userDto.getFirstname() + " " + userDto.getName(), userDto.getEmail()));
		}
		return slimSelectDtos.toArray();
	}

	@GetMapping(value = "/list-ws")
	@ResponseBody
	public String listWs(@ModelAttribute(name = "userEppn") String userEppn, @ModelAttribute(name = "authUserEppn") String authUserEppn,
						 @RequestParam(value = "statusFilter", required = false) String statusFilter,
						 @RequestParam(value = "workflowFilter", required = false) String workflowFilter,
						 @RequestParam(value = "docTitleFilter", required = false) String docTitleFilter,
						 @RequestParam(value = "creatorFilter", required = false) String creatorFilter,
						 @RequestParam(value = "dateFilter", required = false) String dateFilter,
						 @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 10) Pageable pageable, HttpServletRequest httpServletRequest, Model model) {
		if(statusFilter != null && (statusFilter.isEmpty() || statusFilter.equals("all"))) {
			statusFilter = null;
		}
		if(workflowFilter != null && (workflowFilter.isEmpty() || workflowFilter.equals("all"))) {
			workflowFilter = null;
		}
		if(creatorFilter != null && (creatorFilter.isEmpty() || creatorFilter.equals("all"))) {
			creatorFilter = null;
		}
		if(docTitleFilter != null && (docTitleFilter.isEmpty() || docTitleFilter.equals("all"))) {
			docTitleFilter = null;
		}
		Page<SignBook> signBooks = signBookService.getAllSignBooks(statusFilter, workflowFilter, docTitleFilter, creatorFilter, dateFilter, pageable);
		model.addAttribute("signBooks", signBooks);
		CsrfToken token = new HttpSessionCsrfTokenRepository().loadToken(httpServletRequest);
		final Context ctx = new Context(Locale.FRENCH);
		ctx.setVariables(model.asMap());
		ctx.setVariable("_csrf", token);
		return templateEngine.process("admin/signbooks/includes/list-elem.html", ctx);
	}

	@GetMapping(value = "/{id}")
	public String show(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
		SignRequest signRequest = signBookService.search(id);
		if(signRequest != null) {
			return "redirect:/admin/signrequests/" + signRequest.getId();
		}
		redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Demande non trouvée"));
		return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
	}

	@GetMapping(value = "/search")
	public String search(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @RequestParam("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
		SignRequest signRequest = signBookService.search(id);
		if(signRequest != null) {
			return "redirect:/admin/signrequests/" + signRequest.getId();
		}
		redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Demande non trouvée"));
		return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
	}

	@PostMapping(value = "/delete-multiple", consumes = {"application/json"})
	@ResponseBody
	public ResponseEntity<Boolean> deleteMultiple(@ModelAttribute("authUserEppn") String authUserEppn, @RequestBody List<Long> ids, RedirectAttributes redirectAttributes) {
		for(Long id : ids) {
				signBookService.delete(id, authUserEppn);
		}
		redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression effectuée"));
		return new ResponseEntity<>(true, HttpStatus.OK);
	}

	@DeleteMapping(value = "/{id}", produces = "text/html")
	public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
		signBookService.delete(id, authUserEppn);
		redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression effectuée"));
		return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
	}

}
