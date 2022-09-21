package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
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
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

@RequestMapping("/admin/signbooks")
@Controller
public class AdminSignBookController {

	private static final Logger logger = LoggerFactory.getLogger(AdminSignBookController.class);

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
		if(statusFilter == null || statusFilter.isEmpty() || statusFilter.equals("all")) statusFilter = null;
		if(workflowFilter == null || workflowFilter.isEmpty() || workflowFilter.equals("all")) {
			workflowFilter = "%";
		}
		if(creatorFilter == null || creatorFilter.isEmpty() || creatorFilter.equals("all") || creatorFilter.equals("undefined")) {
			creatorFilter = "%";
		}
		if(docTitleFilter == null || docTitleFilter.isEmpty() || docTitleFilter.equals("all")) {
			docTitleFilter = "%";
		}
		Page<SignBook> signBooks = signBookService.getAllSignBooks(statusFilter, workflowFilter, docTitleFilter + "%", creatorFilter, dateFilter, pageable);
		model.addAttribute("statusFilter", statusFilter);
		model.addAttribute("signBooks", signBooks);
		List<User> creators = signBookService.getCreators(null, workflowFilter, docTitleFilter, creatorFilter);
		model.addAttribute("creators", creators);
		model.addAttribute("nbEmpty", signBookService.countEmpty(userEppn));
		model.addAttribute("statuses", SignRequestStatus.values());
		model.addAttribute("forms", formService.getAllForms());
		model.addAttribute("workflows", workflowService.getAllWorkflows());
		model.addAttribute("workflowFilter", workflowFilter);
		if(docTitleFilter != "%") model.addAttribute("docTitleFilter", docTitleFilter);
		model.addAttribute("dateFilter", dateFilter);
		model.addAttribute("workflowNames", signBookRepository.findWorkflowNames());
		return "admin/signbooks/list";
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
		if(statusFilter == null || statusFilter.isEmpty() || statusFilter.equals("all")) statusFilter = null;
		if(workflowFilter == null || workflowFilter.isEmpty() || workflowFilter.equals("all")) {
			workflowFilter = "%";
		}
		if(creatorFilter == null || creatorFilter.isEmpty() || creatorFilter.equals("all")) {
			creatorFilter = "%";
		}
		if(docTitleFilter == null || docTitleFilter.isEmpty() || docTitleFilter.equals("all")) {
			docTitleFilter = "%";
		}
		Page<SignBook> signBooks = signBookService.getAllSignBooks(statusFilter, workflowFilter, docTitleFilter + "%", creatorFilter, dateFilter, pageable);
		model.addAttribute("signBooks", signBooks);
		CsrfToken token = new HttpSessionCsrfTokenRepository().loadToken(httpServletRequest);
		final Context ctx = new Context(Locale.FRENCH);
		ctx.setVariables(model.asMap());
		ctx.setVariable("_csrf", token);
		return templateEngine.process("admin/signbooks/includes/list-elem.html", ctx);
	}

	@PostMapping(value = "/delete-multiple", consumes = {"application/json"})
	@ResponseBody
	public ResponseEntity<Boolean> deleteMultiple(@ModelAttribute("authUserEppn") String authUserEppn, @RequestBody List<Long> ids, RedirectAttributes redirectAttributes) {
		for(Long id : ids) {
				signBookService.delete(id, authUserEppn);
		}
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
		return new ResponseEntity<>(true, HttpStatus.OK);
	}

	@DeleteMapping(value = "/{id}", produces = "text/html")
	public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
		signBookService.delete(id, authUserEppn);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
		return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
	}

}
