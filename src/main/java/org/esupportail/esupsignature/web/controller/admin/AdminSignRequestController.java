package org.esupportail.esupsignature.web.controller.admin;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/admin/signrequests")
@Controller
public class AdminSignRequestController {

	private static final Logger logger = LoggerFactory.getLogger(AdminSignRequestController.class);

	@ModelAttribute("adminMenu")
	public String getAdminMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "adminsignrequests";
	}

	private SignRequestStatus statusFilter = null;

	@Resource
	private SignRequestRepository signRequestRepository;
	
	@Resource
	private SignRequestService signRequestService;

	@Resource
	private DocumentRepository documentRepository;

	@Resource
	private SignBookService signBookService;

	@Resource
	private LogRepository logRepository;

	@Resource
	private FileService fileService;

	@GetMapping
	public String list(
			@RequestParam(value = "statusFilter", required = false) String statusFilter,
			@RequestParam(value = "signBookId", required = false) Long signBookId,
			@RequestParam(value = "messageError", required = false) String messageError,
			@SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 10) Pageable pageable, RedirectAttributes redirectAttributes, Model model) {
		if(statusFilter != null) {
			if(!statusFilter.equals("all")) {
				this.statusFilter = SignRequestStatus.valueOf(statusFilter);
			} else {
				this.statusFilter = null;
			}
		}

		Page<SignRequest> signRequests = signRequestRepository.findBySignResquestByStatus(this.statusFilter,  pageable);

		model.addAttribute("signBookId", signBookId);
		model.addAttribute("signRequests", signRequests);
		model.addAttribute("statusFilter", this.statusFilter);
		model.addAttribute("statuses", SignRequestStatus.values());
		return "admin/signrequests/list";
	}

	@GetMapping(value = "/{id}")
	public String show(@ModelAttribute("user") User user, @PathVariable("id") Long id, Model model) throws Exception {

		SignRequest signRequest = signRequestRepository.findById(id).get();
			model.addAttribute("signBooks", signBookService.getAllSignBooks());
			Document toDisplayDocument = null;
			if(signRequest.getToSignDocuments().size() == 1) {
				toDisplayDocument = signRequest.getToSignDocuments().get(0);
				if(toDisplayDocument.getContentType().equals("application/pdf")) {
				}
				model.addAttribute("documentId", toDisplayDocument.getId());
			}
			List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
			model.addAttribute("logs", logs);
			model.addAttribute("comments", logs.stream().filter(log -> log.getComment() != null && !log.getComment().isEmpty()).collect(Collectors.toList()));
			model.addAttribute("signRequest", signRequest);
			model.addAttribute("itemId", id);
			return "admin/signrequests/show";
	}

	@GetMapping(value = "/getfile/{id}")
	public ResponseEntity<Void> getFile(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
		Document document = documentRepository.findById(id).get();
		response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8.toString()));
		response.setContentType(document.getContentType());
		IOUtils.copy(document.getInputStream(), response.getOutputStream());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping(value = "/{id}", produces = "text/html")
	public String delete(@PathVariable("id") Long id, Model model) {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		signBookService.delete(signRequest.getParentSignBook());

		return "redirect:/admin/signrequests/";
	}

	@GetMapping(value = "/get-last-file/{id}")
	public void getLastFile(@ModelAttribute("user") User user, @PathVariable("id") Long id, HttpServletResponse response, Model model) {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		List<Document> documents = signRequest.getToSignDocuments();
		try {
			if(documents.size() > 1) {
				response.sendRedirect("/user/signrequests/" + id);
			} else {
				Document document = documents.get(0);
				response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8.toString()));
				response.setContentType(document.getContentType());
				IOUtils.copy(document.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
			}
		} catch (Exception e) {
			logger.error("get file error", e);
		}
	}

	@GetMapping(value = "/complete/{id}")
	public String complete(@ModelAttribute("user") User user, @PathVariable("id") Long id,
			@RequestParam(value = "comment", required = false) String comment, HttpServletRequest request) {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		if(signRequest.getCreateBy().equals(user.getEppn()) && (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked))) {
			//signRequestService.completeSignRequest(signRequest, user);
		} else {
			logger.warn(user.getEppn() + " try to complete " + signRequest.getId() + " without rights");
		}
		return "redirect:/admin/signrequests/" + id;
	}

	@GetMapping(value = "/pending/{id}")
	public String pending(@ModelAttribute("user") User user, @PathVariable("id") Long id,
			@RequestParam(value = "comment", required = false) String comment, HttpServletRequest request) {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		signRequest.setComment(comment);
		if(signRequest.getStatus().equals(SignRequestStatus.draft)) {
			signRequestService.updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", "SUCCESS");
		} else {
			logger.warn(user.getEppn() + " try to send for sign " + signRequest.getId() + " without rights");
		}
		return "redirect:/admin/signrequests/" + id;
	}

	@GetMapping(value = "/comment/{id}")
	public String comment(@ModelAttribute("user") User user, @PathVariable("id") Long id,
			@RequestParam(value = "comment", required = false) String comment, RedirectAttributes redirectAttributes, HttpServletRequest request) {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		signRequest.setComment(comment);
		signRequestService.updateStatus(signRequest, null, "Ajout d'un commentaire", "SUCCESS", null, null, null);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Commentaire ajouté"));
		return "redirect:/admin/signrequests/" + id;
	}

}
