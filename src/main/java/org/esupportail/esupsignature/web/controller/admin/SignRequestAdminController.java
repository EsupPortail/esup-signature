package org.esupportail.esupsignature.web.controller.admin;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.dto.mapper.UiFetchService;
import org.esupportail.esupsignature.dto.mapper.UiFetchSignRequestService;
import org.esupportail.esupsignature.dto.page.admin.AdminSignRequestShowViewDto;
import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/admin/signrequests")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class SignRequestAdminController {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestAdminController.class);
	private final UiFetchSignRequestService uiFetchSignRequestService;

	@ModelAttribute("adminMenu")
	public String getAdminMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "adminsignrequests";
	}

	private final SignBookRepository signBookRepository;
	private final DocumentRepository documentRepository;
	private final SignBookService signBookService;
	private final UiFetchService uiFetchService;

	public SignRequestAdminController(SignBookRepository signBookRepository, DocumentRepository documentRepository, SignBookService signBookService, UiFetchService uiFetchService, UiFetchSignRequestService uiFetchSignRequestService) {
		this.signBookRepository = signBookRepository;
		this.documentRepository = documentRepository;
		this.signBookService = signBookService;
		this.uiFetchService = uiFetchService;
		this.uiFetchSignRequestService = uiFetchSignRequestService;
	}

	@GetMapping
	public String list(
			@RequestParam(value = "statusFilter", required = false) String statusFilter,
			@RequestParam(value = "signBookId", required = false) Long signBookId,
			@SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 12) Pageable pageable, Model model) {
		Page<SignBook> signBooks;
		if(statusFilter == null || statusFilter.isEmpty() || statusFilter.equals("all")) {
			signBooks = signBookRepository.findAll(pageable);
		} else {
			signBooks = signBookRepository.findByStatus(SignRequestStatus.valueOf(statusFilter), pageable);
		}

		model.addAttribute("signBookId", signBookId);
		model.addAttribute("signBooks", signBooks);
		model.addAttribute("statusFilter", statusFilter);
		model.addAttribute("statuses", SignRequestStatus.activeValues());
		return "admin/signbooks/list";
	}

	@GetMapping(value = "/{id}")
	public String show(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		AdminSignRequestShowViewDto view = uiFetchSignRequestService.buildAdminSignRequestShowView(id);
		if(view != null) {
			model.addAttribute("adminSignRequestView", view);
			model.addAttribute("signRequestLight", view.signRequestLight());
			model.addAttribute("signRequestFull", view.signRequestFull());
			model.addAttribute("signBookLight", view.signBookLight());
			model.addAttribute("workflow", view.workflow());
			model.addAttribute("steps", view.steps());
			model.addAttribute("targets", view.targets());
			model.addAttribute("comments", view.comments());
			model.addAttribute("logs", view.logs());
			model.addAttribute("originalDocuments", view.originalDocuments());
			model.addAttribute("signedDocuments", view.signedDocuments());
			model.addAttribute("documentsHistory", view.documentsHistory());
			model.addAttribute("isManager", view.manager());
			return "admin/signrequests/show";
		} else {
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "La demande de signature n'existe pas"));
			return "redirect:/admin/signrequests";
		}
	}

	@GetMapping(value = "/getfile/{id}")
	public ResponseEntity<Void> getFile(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
		Document document = documentRepository.findById(id).get();
		response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8).replace("+", "%20"));
		response.setContentType(document.getContentType());
		IOUtils.copy(document.getInputStream(), response.getOutputStream());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping(value = "delete-definitive/{id}", produces = "text/html")
	public String deleteDefinitive(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
		if(signBookService.deleteDefinitive(id, authUserEppn)) {
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Le document a été supprimé définitivement"));
		} else {
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Le document ne peut pas être supprimé définitivement"));
		}
		return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
	}

}
