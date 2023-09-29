package org.esupportail.esupsignature.web.controller.admin;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
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
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/admin/signrequests")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class SignRequestAdminController {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestAdminController.class);

	@Resource
	private SignService signService;

	@Resource
	private WebUtilsService webUtilsService;

	@ModelAttribute("adminMenu")
	public String getAdminMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "adminsignrequests";
	}

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private SignBookRepository signBookRepository;

	@Resource
	private DocumentRepository documentRepository;

	@Resource
	private SignBookService signBookService;

	@Resource
	private LogService logService;

	@GetMapping
	public String list(
			@RequestParam(value = "statusFilter", required = false) String statusFilter,
			@RequestParam(value = "signBookId", required = false) Long signBookId,
			@SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
		Page<SignBook> signBooks;
		if(statusFilter == null || statusFilter.isEmpty() || statusFilter.equals("all")) {
			signBooks = signBookRepository.findAll(pageable);
		} else {
			signBooks = signBookRepository.findByStatus(SignRequestStatus.valueOf(statusFilter), pageable);
		}

		model.addAttribute("signBookId", signBookId);
		model.addAttribute("signBooks", signBooks);
		model.addAttribute("statusFilter", statusFilter);
		model.addAttribute("statuses", SignRequestStatus.values());
		return "admin/signbooks/list";
	}

	@GetMapping(value = "/{id}")
	@Transactional
	public String show(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model) {
		SignRequest signRequest = signRequestService.getById(id);
		List<Log> logs = logService.getBySignRequestId(signRequest.getId());
		model.addAttribute("logs", logs);
		model.addAttribute("comments", logs.stream().filter(log -> log.getComment() != null && !log.getComment().isEmpty()).collect(Collectors.toList()));
		model.addAttribute("signRequest", signRequest);
		model.addAttribute("originalDocuments", signRequest.getOriginalDocuments());
		model.addAttribute("signedDocuments", signRequest.getSignedDocuments());
		return "admin/signrequests/show";
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
			redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le document a été supprimé définitivement"));
		} else {
			redirectAttributes.addFlashAttribute("warn", new JsonMessage("info", "Le document ne peut pas être supprimé définitivement"));
		}
		return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
	}

	@GetMapping(value = "/get-last-file/{id}")
	@Transactional
	public void getLastFile(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
		List<Document> documents = signService.getToSignDocuments(id);
		try {
			if(documents.size() > 1) {
				httpServletResponse.sendRedirect("/user/signrequests/" + id);
			} else {
				Document document = documents.get(0);
				webUtilsService.copyFileStreamToHttpResponse(document.getFileName(), document.getContentType(), "attachment", document.getInputStream(), httpServletResponse);
			}
		} catch (Exception e) {
			logger.error("get file error", e);
		}
	}

	@GetMapping(value = "/restore/{id}", produces = "text/html")
	public String restore(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		signRequestService.restore(id, authUserEppn);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Restauration effectuée"));
		return "redirect:/user/signrequests/" + id;
	}

}
